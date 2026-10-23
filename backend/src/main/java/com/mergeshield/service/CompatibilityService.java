
package com.mergeshield.service;

import com.mergeshield.api.AnalysisResponse;
import com.mergeshield.api.Finding;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CompatibilityService {

    private static final List<PathItem.HttpMethod> METHODS = List.of(
        PathItem.HttpMethod.GET,
        PathItem.HttpMethod.POST,
        PathItem.HttpMethod.PUT,
        PathItem.HttpMethod.PATCH,
        PathItem.HttpMethod.DELETE,
        PathItem.HttpMethod.OPTIONS,
        PathItem.HttpMethod.HEAD,
        PathItem.HttpMethod.TRACE
    );

    public AnalysisResponse analyze(String oldText, String newText) {
        OpenAPI oldApi = parse(oldText, "oldSpec");
        OpenAPI newApi = parse(newText, "newSpec");

        List<Finding> findings = new ArrayList<>();

        Map<String, PathItem> oldPaths = safe(oldApi.getPaths());
        Map<String, PathItem> newPaths = safe(newApi.getPaths());

        for (var entry : oldPaths.entrySet()) {
            String path = entry.getKey();
            PathItem oldItem = entry.getValue();
            PathItem newItem = newPaths.get(path);

            if (newItem == null) {
                findings.add(new Finding(
                    "PATH_REMOVED", "BREAKING", path,
                    "An API path was removed.",
                    "present", "missing", "paths." + path
                ));
                continue;
            }

            for (PathItem.HttpMethod method : METHODS) {
                Operation oldOp = oldItem.readOperationsMap().get(method);

                if (oldOp == null) {
                    continue;
                }

                Operation newOp = newItem.readOperationsMap().get(method);
                String location = method.name() + " " + path;

                if (newOp == null) {
                    findings.add(new Finding(
                        "METHOD_REMOVED", "BREAKING", location,
                        "An HTTP operation was removed.",
                        "present", "missing",
                        "paths." + path + "."
                            + method.name().toLowerCase(Locale.ROOT)
                    ));
                    continue;
                }

                compareParameters(
                    oldItem, oldOp, newItem, newOp, location, findings
                );

                compareRequestBody(oldOp, newOp, location, findings);

                compareRequiredRequestProperties(
                    oldOp, newOp, location, findings
                );

                compareResponses(oldOp, newOp, location, findings);

                compareInlineProperties(
                    oldOp, newOp, location, findings
                );
            }
        }

        int breaking = (int) findings.stream()
            .filter(f -> f.severity().equals("BREAKING"))
            .count();

        return new AnalysisResponse(
            "completed",
            breaking,
            findings.size() - breaking,
            List.copyOf(findings)
        );
    }

    private OpenAPI parse(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                field + " must not be empty."
            );
        }

        try {
            ParseOptions options = new ParseOptions();
            options.setResolve(false);

            SwaggerParseResult result =
                new OpenAPIV3Parser().readContents(text, null, options);

            if (result == null || result.getOpenAPI() == null) {
                throw new IllegalArgumentException(
                    field + " is invalid. Provide a valid OpenAPI 3.x document."
                );
            }

            OpenAPI api = result.getOpenAPI();

            if (api.getOpenapi() == null
                    || !api.getOpenapi().startsWith("3.")) {
                throw new IllegalArgumentException(
                    field + " must use OpenAPI 3.x."
                );
            }

            if (api.getInfo() == null) {
                throw new IllegalArgumentException(
                    field + " must include an info object."
                );
            }

            if (api.getPaths() == null) {
                throw new IllegalArgumentException(
                    field + " must include a paths object."
                );
            }

            return api;

        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                field + " is invalid. Provide a valid OpenAPI 3.x document."
            );
        }
    }

    private void compareParameters(
        PathItem oldItem,
        Operation oldOp,
        PathItem newItem,
        Operation newOp,
        String location,
        List<Finding> out
    ) {
        Map<String, Parameter> oldParams = parameterMap(
            mergeParameters(oldItem.getParameters(), oldOp.getParameters())
        );

        Map<String, Parameter> newParams = parameterMap(
            mergeParameters(newItem.getParameters(), newOp.getParameters())
        );

        for (var entry : newParams.entrySet()) {
            Parameter current = entry.getValue();
            Parameter previous = oldParams.get(entry.getKey());

            if (Boolean.TRUE.equals(current.getRequired())
                    && (previous == null
                    || !Boolean.TRUE.equals(previous.getRequired()))) {
                out.add(new Finding(
                    "PARAMETER_NOW_REQUIRED", "BREAKING", location,
                    "A parameter is now required or was newly introduced as required.",
                    previous == null ? "not present" : "optional",
                    "required",
                    "parameters[" + entry.getKey() + "].required"
                ));
            }
        }
    }

    private List<Parameter> mergeParameters(
        List<Parameter> pathParams,
        List<Parameter> operationParams
    ) {
        Map<String, Parameter> merged = new LinkedHashMap<>();

        addParameters(merged, pathParams);
        addParameters(merged, operationParams);

        return new ArrayList<>(merged.values());
    }

    private void addParameters(
        Map<String, Parameter> target,
        List<Parameter> parameters
    ) {
        if (parameters == null) {
            return;
        }

        for (Parameter parameter : parameters) {
            if (parameter != null
                    && parameter.getName() != null
                    && parameter.getIn() != null) {
                target.put(
                    parameter.getIn() + ":" + parameter.getName(),
                    parameter
                );
            }
        }
    }

    private Map<String, Parameter> parameterMap(
        List<Parameter> parameters
    ) {
        Map<String, Parameter> map = new HashMap<>();
        addParameters(map, parameters);
        return map;
    }

    private void compareRequestBody(
        Operation oldOp,
        Operation newOp,
        String location,
        List<Finding> out
    ) {
        boolean oldRequired =
            oldOp.getRequestBody() != null
                && Boolean.TRUE.equals(oldOp.getRequestBody().getRequired());

        boolean newRequired =
            newOp.getRequestBody() != null
                && Boolean.TRUE.equals(newOp.getRequestBody().getRequired());

        if (!oldRequired && newRequired) {
            out.add(new Finding(
                "REQUEST_BODY_NOW_REQUIRED", "BREAKING", location,
                "The request body is now required.",
                oldOp.getRequestBody() == null ? "not present" : "optional",
                "required",
                "requestBody.required"
            ));
        }
    }

    /**
     * Detects newly required top-level properties in inline request-body
     * schemas. Referenced and nested schemas are outside this MVP's scope.
     */
    private void compareRequiredRequestProperties(
        Operation oldOp,
        Operation newOp,
        String location,
        List<Finding> out
    ) {
        if (oldOp.getRequestBody() == null
                || newOp.getRequestBody() == null) {
            return;
        }

        Content oldContent = oldOp.getRequestBody().getContent();
        Content newContent = newOp.getRequestBody().getContent();

        if (oldContent == null || newContent == null) {
            return;
        }

        for (var entry : newContent.entrySet()) {
            var newMedia = entry.getValue();
            var oldMedia = oldContent.get(entry.getKey());

            if (newMedia == null
                    || newMedia.getSchema() == null
                    || oldMedia == null
                    || oldMedia.getSchema() == null) {
                continue;
            }

            Schema<?> oldSchema = oldMedia.getSchema();
            Schema<?> newSchema = newMedia.getSchema();

            if (oldSchema.get$ref() != null
                    || newSchema.get$ref() != null) {
                continue;
            }

            Set<String> oldRequired =
                oldSchema.getRequired() == null
                    ? Collections.emptySet()
                    : new HashSet<>(oldSchema.getRequired());

            Set<String> newRequired =
                newSchema.getRequired() == null
                    ? Collections.emptySet()
                    : new HashSet<>(newSchema.getRequired());

            for (String property : newRequired) {
                if (!oldRequired.contains(property)) {
                    out.add(new Finding(
                        "REQUEST_PROPERTY_NOW_REQUIRED", "BREAKING", location,
                        "A request body property is now required; existing clients may omit it.",
                        "optional/not required",
                        "required",
                        "requestBody.content." + entry.getKey()
                            + ".schema.required." + property
                    ));
                }
            }
        }
    }

    private void compareResponses(
        Operation oldOp,
        Operation newOp,
        String location,
        List<Finding> out
    ) {
        Map<String, ApiResponse> oldResponses = safe(oldOp.getResponses());
        Map<String, ApiResponse> newResponses = safe(newOp.getResponses());

        for (String code : oldResponses.keySet()) {
            if (!newResponses.containsKey(code)) {
                out.add(new Finding(
                    "RESPONSE_REMOVED", "BREAKING", location,
                    "A previously documented response status was removed.",
                    code, "missing", "responses." + code
                ));
            }
        }
    }

    private void compareInlineProperties(
        Operation oldOp,
        Operation newOp,
        String location,
        List<Finding> out
    ) {
        Map<String, Schema<?>> oldSchemas = inlineSchemas(oldOp);
        Map<String, Schema<?>> newSchemas = inlineSchemas(newOp);

        for (var entry : oldSchemas.entrySet()) {
            Schema<?> oldSchema = entry.getValue();
            Schema<?> newSchema = newSchemas.get(entry.getKey());

            if (newSchema == null) {
                continue;
            }

            Map<String, Schema<?>> oldProperties =
                schemaProperties(oldSchema);
            Map<String, Schema<?>> newProperties =
                schemaProperties(newSchema);

            for (String property : oldProperties.keySet()) {
                if (!newProperties.containsKey(property)) {
                    out.add(new Finding(
                        "INLINE_PROPERTY_REMOVED", "WARNING", location,
                        "An inline schema property was removed; compatibility depends on client usage.",
                        property,
                        "missing",
                        entry.getKey() + ".properties." + property
                    ));
                }
            }
        }
    }

    private Map<String, Schema<?>> inlineSchemas(Operation operation) {
        Map<String, Schema<?>> result = new HashMap<>();

        if (operation.getRequestBody() != null
                && operation.getRequestBody().getContent() != null) {
            addContent(
                result, "requestBody",
                operation.getRequestBody().getContent()
            );
        }

        if (operation.getResponses() != null) {
            operation.getResponses().forEach((status, response) -> {
                if (response != null && response.getContent() != null) {
                    addContent(
                        result,
                        "responses." + status,
                        response.getContent()
                    );
                }
            });
        }

        return result;
    }

    private void addContent(
        Map<String, Schema<?>> result,
        String prefix,
        Content content
    ) {
        content.forEach((mediaType, media) -> {
            if (media != null
                    && media.getSchema() != null
                    && media.getSchema().getProperties() != null) {
                result.put(
                    prefix + "." + mediaType,
                    media.getSchema()
                );
            }
        });
    }

    private Map<String, Schema<?>> schemaProperties(Schema<?> schema) {
        Map<String, Schema<?>> result = new HashMap<>();
        Map<String, Schema> properties = safe(schema.getProperties());

        properties.forEach((name, propertySchema) -> {
            if (propertySchema != null) {
                result.put(name, propertySchema);
            }
        });

        return result;
    }

    private <T> Map<String, T> safe(Map<String, T> map) {
        return map == null ? Collections.emptyMap() : map;
    }
}
