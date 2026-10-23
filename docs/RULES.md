# Compatibility rules in the MVP

| Rule ID | Severity | Behavior |
|---|---|---|
| `PATH_REMOVED` | BREAKING | A path present in the old document is absent in the new document. |
| `METHOD_REMOVED` | BREAKING | An HTTP operation present on a path is absent in the new document. |
| `PARAMETER_NOW_REQUIRED` | BREAKING | A parameter is newly introduced as required or changes from optional to required. Operation-level parameters override path-level parameters with the same `in` and `name`. |
| `REQUEST_BODY_NOW_REQUIRED` | BREAKING | A request body changes from absent/optional to required. |
| `REQUEST_PROPERTY_NOW_REQUIRED` | BREAKING | A directly declared inline request-body property becomes required. Referenced and nested schemas are not traversed. |
| `RESPONSE_REMOVED` | BREAKING | A response status code documented previously is absent from the new operation. |
| `INLINE_PROPERTY_REMOVED` | WARNING | A property in an inline request/response schema disappears. Impact depends on where the schema is used. |

## Known limitations

- Referenced (`$ref`) schemas are not fully traversed or diffed.
- Inline property comparison only checks schemas with directly declared properties. Nested object properties, arrays, `allOf`/`oneOf`/`anyOf`, enums, formats, type changes, required-property lists beyond directly declared request-body properties, parameter schema changes, security requirements, and response content-type changes are not fully covered.
- The current response-status rule compares literal response keys; more advanced semantic handling may be needed for `default` and status-code ranges.
- A finding is a compatibility signal, not proof of runtime impact. A clean report is not a guarantee of compatibility.
