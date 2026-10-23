# MergeShield

**Find API compatibility risks before shipping.** MergeShield compares two OpenAPI 3.x specifications and reports a starter set of potential compatibility risks.

## Stack
- Java 21, Spring Boot, Maven
- Swagger Parser / OpenAPI models
- React, TypeScript, Vite
- GitHub Actions CI
- Docker Compose for running the stateless backend locally

## Quick start
See [`DEPLOYMENT.md`](DEPLOYMENT.md) for local setup, tests, GitHub, Render, and Vercel instructions. See [`VERIFICATION.md`](VERIFICATION.md) for what has and has not been verified in this packaging environment.

Backend: `cd backend && mvn test && mvn spring-boot:run`

Frontend in another terminal: `cd frontend && npm install && npm run build && npm run dev`

Health check: `http://localhost:8080/api/health`

Optional Docker run from the project root: `docker compose up --build`

## Current MVP limitations
The first rules detect removed paths, removed HTTP methods, newly required operation- or path-level parameters, newly required request bodies or direct inline request properties, removed response statuses, and removed properties from inline request/response object schemas. `$ref` graphs, full OpenAPI semantic diffing, analysis history, authentication, GitHub integration, and billing are not implemented. Findings are specification-level signals, not proof of runtime failure.

**Do not use this MVP as a production release gate yet.** Add rule coverage and tests before relying on its results.
