
package com.mergeshield.service;

import com.mergeshield.api.AnalysisResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompatibilityServiceTest {

    private final CompatibilityService service = new CompatibilityService();

    @Test
    void detectsRemovedPath() {
        String oldSpec = """
            {"openapi":"3.0.3","info":{"title":"Old","version":"1.0"},"paths":{"/users":{"get":{"responses":{"200":{"description":"OK"}}}}}}
            """;

        String newSpec = """
            {"openapi":"3.0.3","info":{"title":"New","version":"2.0"},"paths":{}}
            """;

        AnalysisResponse result = service.analyze(oldSpec, newSpec);

        assertEquals(1, result.breakingCount());
        assertEquals("PATH_REMOVED", result.findings().get(0).ruleId());
    }

    @Test
    void detectsRemovedMethod() {
        String oldSpec = """
            {"openapi":"3.0.3","info":{"title":"Old","version":"1.0"},"paths":{"/users":{"get":{"responses":{"200":{"description":"OK"}}}}}}
            """;

        String newSpec = """
            {"openapi":"3.0.3","info":{"title":"New","version":"2.0"},"paths":{"/users":{}}}
            """;

        AnalysisResponse result = service.analyze(oldSpec, newSpec);

        assertEquals(1, result.breakingCount());
        assertEquals("METHOD_REMOVED", result.findings().get(0).ruleId());
    }

    @Test
    void detectsParameterBecomingRequired() {
        String oldSpec = """
            {"openapi":"3.0.3","info":{"title":"Old","version":"1.0"},"paths":{"/users":{"get":{"parameters":[{"name":"page","in":"query","required":false,"schema":{"type":"integer"}}],"responses":{"200":{"description":"OK"}}}}}}
            """;

        String newSpec = """
            {"openapi":"3.0.3","info":{"title":"New","version":"2.0"},"paths":{"/users":{"get":{"parameters":[{"name":"page","in":"query","required":true,"schema":{"type":"integer"}}],"responses":{"200":{"description":"OK"}}}}}}
            """;

        AnalysisResponse result = service.analyze(oldSpec, newSpec);

        assertEquals(1, result.breakingCount());
        assertEquals("PARAMETER_NOW_REQUIRED", result.findings().get(0).ruleId());
    }

    @Test
    void rejectsInvalidSpec() {
        String valid = """
            {"openapi":"3.0.3","info":{"title":"x","version":"1"},"paths":{}}
            """;

        assertThrows(
            IllegalArgumentException.class,
            () -> service.analyze("not a spec", valid)
        );
    }

    @Test
    void detectsRemovedResponseStatus() {
        String oldSpec = """
            {"openapi":"3.0.3","info":{"title":"Old","version":"1"},"paths":{"/users":{"get":{"responses":{"200":{"description":"OK"},"404":{"description":"Missing"}}}}}}
            """;

        String newSpec = """
            {"openapi":"3.0.3","info":{"title":"New","version":"2"},"paths":{"/users":{"get":{"responses":{"200":{"description":"OK"}}}}}}
            """;

        AnalysisResponse result = service.analyze(oldSpec, newSpec);

        assertTrue(result.findings().stream().anyMatch(
            f -> f.ruleId().equals("RESPONSE_REMOVED")
                && f.evidence().equals("responses.404")
        ));

        assertEquals(1, result.breakingCount());
    }

    @Test
    void detectsRequestBodyBecomingRequired() {
        String oldSpec = """
            {"openapi":"3.0.3","info":{"title":"Old","version":"1"},"paths":{"/users":{"post":{"requestBody":{"required":false,"content":{"application/json":{"schema":{"type":"object"}}}},"responses":{"200":{"description":"OK"}}}}}}
            """;

        String newSpec = """
            {"openapi":"3.0.3","info":{"title":"New","version":"2"},"paths":{"/users":{"post":{"requestBody":{"required":true,"content":{"application/json":{"schema":{"type":"object"}}}},"responses":{"200":{"description":"OK"}}}}}}
            """;

        AnalysisResponse result = service.analyze(oldSpec, newSpec);

        assertTrue(result.findings().stream().anyMatch(
            f -> f.ruleId().equals("REQUEST_BODY_NOW_REQUIRED")
        ));

        assertEquals(1, result.breakingCount());
    }

    @Test
    void detectsNewlyRequiredPathLevelParameter() {
        String oldSpec = """
            {"openapi":"3.0.3","info":{"title":"Old","version":"1"},"paths":{"/users":{"parameters":[{"name":"tenant","in":"header","required":false,"schema":{"type":"string"}}],"get":{"responses":{"200":{"description":"OK"}}}}}}
            """;

        String newSpec = """
            {"openapi":"3.0.3","info":{"title":"New","version":"2"},"paths":{"/users":{"parameters":[{"name":"tenant","in":"header","required":true,"schema":{"type":"string"}}],"get":{"responses":{"200":{"description":"OK"}}}}}}
            """;

        AnalysisResponse result = service.analyze(oldSpec, newSpec);

        assertTrue(result.findings().stream().anyMatch(
            f -> f.ruleId().equals("PARAMETER_NOW_REQUIRED")
        ));

        assertEquals(1, result.breakingCount());
    }

    @Test
    void detectsRequestPropertyBecomingRequired() {
        String oldSpec = """
            {"openapi":"3.0.3","info":{"title":"Old","version":"1"},"paths":{"/users":{"post":{"requestBody":{"required":true,"content":{"application/json":{"schema":{"type":"object","properties":{"name":{"type":"string"}}}}}},"responses":{"200":{"description":"OK"}}}}}}
            """;

        String newSpec = """
            {"openapi":"3.0.3","info":{"title":"New","version":"2"},"paths":{"/users":{"post":{"requestBody":{"required":true,"content":{"application/json":{"schema":{"type":"object","required":["name"],"properties":{"name":{"type":"string"}}}}}},"responses":{"200":{"description":"OK"}}}}}}
            """;

        AnalysisResponse result = service.analyze(oldSpec, newSpec);

        assertTrue(result.findings().stream().anyMatch(
            f -> f.ruleId().equals("REQUEST_PROPERTY_NOW_REQUIRED")
        ));

        assertEquals(1, result.breakingCount());
    }

    @Test
    void doesNotFlagAnAlreadyRequiredRequestProperty() {
        String spec = """
            {"openapi":"3.0.3","info":{"title":"API","version":"1"},"paths":{"/users":{"post":{"requestBody":{"required":true,"content":{"application/json":{"schema":{"type":"object","required":["name"],"properties":{"name":{"type":"string"}}}}}},"responses":{"200":{"description":"OK"}}}}}}
            """;

        AnalysisResponse result = service.analyze(spec, spec);

        assertEquals(0, result.findings().size());
    }

    @Test
    void warnsWhenResponsePropertyIsRemoved() {
        String oldSpec = """
            {"openapi":"3.0.3","info":{"title":"Old","version":"1"},"paths":{"/users":{"get":{"responses":{"200":{"description":"OK","content":{"application/json":{"schema":{"type":"object","properties":{"id":{"type":"integer"},"name":{"type":"string"}}}}}}}}}}}
            """;

        String newSpec = """
            {"openapi":"3.0.3","info":{"title":"New","version":"2"},"paths":{"/users":{"get":{"responses":{"200":{"description":"OK","content":{"application/json":{"schema":{"type":"object","properties":{"id":{"type":"integer"}}}}}}}}}}}
            """;

        AnalysisResponse result = service.analyze(oldSpec, newSpec);

        assertTrue(result.findings().stream().anyMatch(
            f -> f.ruleId().equals("INLINE_PROPERTY_REMOVED")
                && f.severity().equals("WARNING")
                && f.evidence().equals(
                    "responses.200.application/json.properties.name"
                )
        ));

        assertEquals(0, result.breakingCount());
    }

    @Test
    void doesNotFlagAddedResponseProperty() {
        String oldSpec = """
            {"openapi":"3.0.3","info":{"title":"API","version":"1"},"paths":{"/users":{"get":{"responses":{"200":{"description":"OK","content":{"application/json":{"schema":{"type":"object","properties":{"id":{"type":"integer"}}}}}}}}}}}
            """;

        String newSpec = """
            {"openapi":"3.0.3","info":{"title":"API","version":"2"},"paths":{"/users":{"get":{"responses":{"200":{"description":"OK","content":{"application/json":{"schema":{"type":"object","properties":{"id":{"type":"integer"},"email":{"type":"string"}}}}}}}}}}}
            """;

        AnalysisResponse result = service.analyze(oldSpec, newSpec);

        assertEquals(0, result.breakingCount());
        assertTrue(result.findings().isEmpty());
    }
}
