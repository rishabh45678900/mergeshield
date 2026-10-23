# Architecture overview

## Request flow

1. React collects the old and proposed OpenAPI document text.
2. The browser sends `POST /api/analyze` with `{ "oldSpec": "...", "newSpec": "..." }`.
3. Spring validates the request size and required fields.
4. Swagger Parser parses both OpenAPI 3.x documents.
5. `CompatibilityService` compares the supported parts and emits structured findings.
6. React shows counts, rule IDs, affected locations, and old/new evidence.

## Main backend files

- `api/AnalysisController.java`: HTTP endpoints.
- `api/AnalysisRequest.java`: request validation.
- `api/Finding.java`: one finding returned by a rule.
- `api/AnalysisResponse.java`: report summary.
- `api/ApiExceptionHandler.java`: readable client errors.
- `service/CompatibilityService.java`: initial comparison rules.
- `config/CorsConfig.java`: frontend-origin restriction.

## Design choices

The analysis engine is a deterministic rules engine, not an AI judgment. Every finding should be tied to a rule ID and a location in the API document. A rule can be a false positive or miss a risk; test rules against representative specifications before using them to block a release.

The current version is stateless. It does not write specifications to a database. This reduces the risk of storing sensitive API definitions while the MVP is being validated.
