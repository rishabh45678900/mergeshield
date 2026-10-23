# Product roadmap

## Phase 0 — starter MVP (current)
- [x] Compare two OpenAPI 3.x documents in the browser
- [x] Initial deterministic compatibility rules
- [x] Report findings and evidence
- [x] Initial backend tests and CI workflow
- [x] Docker-based backend deployment starter

## Phase 1 — trust the analysis
- [ ] Expand tests with realistic OpenAPI fixtures
- [ ] Resolve local component `$ref` schemas safely
- [x] Compare path-level and operation-level parameters, with operation-level overrides
- [ ] Detect required request fields, response changes, type/format changes, enum narrowing, and request/response content-type changes
- [x] Add stable rule documentation and user-facing caveat text
- [ ] Add upload-size, time, and parser-resource limits

## Phase 2 — developer workflow
- [x] Import JSON/YAML files in addition to paste-in text
- [x] Download a JSON report
- [ ] Add example projects and sample reports
- [ ] Collect opt-in feedback on false positives and missed risks

## Phase 3 — accounts and team use
- [ ] Add PostgreSQL schema and migrations
- [ ] Add authentication and authorization tests
- [ ] Add history, retention controls, and deletion/export flows
- [ ] Add per-user quotas and abuse protection

## Phase 4 — distribution and monetization
- [ ] Build a GitHub App with least-privilege permissions
- [ ] Run checks on pull requests and publish status/comments
- [ ] Interview users and confirm recurring demand
- [ ] Measure retention and willingness to pay before introducing subscriptions
- [ ] Add billing only after usage limits, support, and data handling are ready

## Definition of done for a release
- All tests pass in CI
- Build is reproducible
- User-visible limitations are documented
- No secrets are committed
- Security and data-handling changes are reviewed
- Deployment is smoke-tested after release
