# MergeShield — final setup and deployment guide

This repository is an early MVP, not a production-ready paid SaaS yet. Deploy the demo first, then add persistence, authentication, usage limits, and billing only after tests and user validation.

## 1. Requirements

Install Java 21, Maven 3.9+, Node.js 22+, Git, and VS Code. Docker is optional. The Compose file runs the stateless API; no database is required by this MVP.

## 2. Run locally

Terminal A (backend):

```bash
cd backend
mvn test
mvn spring-boot:run
```

Check `http://localhost:8080/api/health`; expected JSON includes `"status":"ok"`. The sample comparison documents are in `examples/old-openapi.json` and `examples/new-openapi.json`.

Terminal B (frontend):

```bash
cd frontend
npm install
npm run build
npm run dev
```

Open the Vite URL shown in the terminal, normally `http://localhost:5173`. Select **Load demo**, then **Run compatibility check**. The demo should show a removed `/orders` path.

If the backend is hosted somewhere other than localhost, copy `frontend/.env.example` to `frontend/.env.local` and set `VITE_API_BASE_URL` to the backend base URL, without a trailing slash. Restart the frontend after changing environment variables.

Optional Docker run from the project root:

```bash
docker compose up --build
```

The API health endpoint will be available at `http://localhost:8080/api/health`.

## 3. Push to GitHub

Create an empty repository named `mergeshield` on GitHub. From the project root: 

```bash
git init
git add .
git commit -m "Build MergeShield API compatibility MVP"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/mergeshield.git
git push -u origin main
```

Replace `YOUR_USERNAME`. Never commit passwords, API keys, or real `.env` files. The included GitHub Actions workflow runs backend tests and a frontend production build on pushes and pull requests.

## 4. Deploy backend to Render with Docker

1. Push the repository to GitHub and connect GitHub to Render.
2. Create **New → Web Service** and select this repository.
3. Choose Docker runtime and set **Root Directory** to `backend`; use `Dockerfile` as the Dockerfile path. This ensures the Docker build context contains `pom.xml` and `src/`.
4. Add environment variable `CORS_ALLOWED_ORIGIN` with the exact deployed frontend origin, for example `https://your-project.vercel.app` (no trailing slash). Initially, you can use the localhost origin for local development only.
5. Deploy, then open `https://YOUR-SERVICE.onrender.com/api/health`.
6. Free web services may sleep after inactivity and take time to wake up; this can make the first request slow. Check current plan limits before relying on it for users.

The Docker build runs `mvn test package`; a failing test should fail the deployment.

## 5. Deploy frontend to Vercel

1. Import the GitHub repository into Vercel.
2. Set **Root Directory** to `frontend`.
3. Use build command `npm run build` and output directory `dist` (Vercel usually detects Vite automatically).
4. Add environment variable `VITE_API_BASE_URL` set to your Render backend base URL, e.g. `https://YOUR-SERVICE.onrender.com`. This is a public API URL, not a secret.
5. Deploy. Copy the final frontend origin and update the backend's `CORS_ALLOWED_ORIGIN` on Render to that exact origin, then redeploy the backend.
6. Open the frontend and run the demo analysis. If it fails, inspect the browser console/network tab and Render logs.

References: [Vite static deployment](https://vite.dev/guide/static-deploy.html), [Vercel Vite deployment](https://vercel.com/docs/frameworks/frontend/vite), [Render deployment](https://render.com/docs/your-first-deploy), [Render Docker](https://render.com/docs/docker).

## 6. Test the API directly

With the backend running, use PowerShell or a terminal with `curl` available:

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  --data-binary '{"oldSpec":"{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"Old\",\"version\":\"1.0\"},\"paths\":{\"/users\":{\"get\":{\"responses\":{\"200\":{\"description\":\"OK\"}}}}}}","newSpec":"{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"New\",\"version\":\"2.0\"},\"paths\":{}}"}'
```

Expected: a completed report with a `PATH_REMOVED` breaking finding. If this command is awkward in PowerShell, use the browser demo instead.

## 7. Verify before sharing

- [ ] `mvn test` passes locally.
- [ ] `npm run build` passes locally.
- [ ] `/api/health` returns HTTP 200.
- [ ] Valid specs return a completed report.
- [ ] Invalid specs return HTTP 400 with a readable error.
- [ ] The frontend can call the deployed API without a CORS error.
- [ ] A mobile viewport remains usable.
- [ ] README explains known limitations and rule behavior.

## 8. Current limits — do not hide these

- Only a starter set of compatibility rules is implemented.
- `$ref` schema graphs are not fully analyzed; inline schema support is limited.
- Analysis history, accounts, teams, GitHub pull-request checks, subscription billing, and rate limiting are not implemented.
- Findings are specification-level signals, not proof that runtime clients will fail.
- Do not upload confidential/private API specifications until privacy, retention, and access controls are designed and tested.

## 9. Build next in this order

1. Expand rule coverage and tests (required request fields, enum/type changes, response schema changes, `$ref` resolution). Path-level required parameters, removed response statuses, and newly required request bodies have initial coverage.
2. Add structured error handling, request limits, logging, and security review.
3. Add PostgreSQL persistence and migration tooling.
4. Add authentication, per-user quotas, and deletion/export controls.
5. Add GitHub App integration and pull-request comments only after core rules are stable.
6. Interview API developers and get repeat usage before building paid tiers.
7. Add billing last; never claim customers or revenue before they exist.
