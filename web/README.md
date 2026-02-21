# Web UI

Next.js web application for Auctor. It provides the UI and server routes that proxy GraphQL requests to backend services.

## What it does
- Login flow (`/login`) with Google Sign-In and optional dev login.
- Proxies backend calls via:
  - `/api/definition-graphql` -> definition-service `/graphql`
  - `/api/execution-graphql` -> execution-service `/graphql`
- Stores browser session token and sends `Authorization` to backend APIs.

## Local Run (Standalone)
From `web/`:

```bash
cp .env.example .env.local
npm install
npm run dev
```

Open `http://localhost:3000`.

## Required Environment Variables
See `.env.example` for the full list. Required for the standard local flow:
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_ID`
- `DEFINITION_SERVICE_URL`
- `EXECUTION_SERVICE_URL`

JWT-related values are already provided in `.env.example` for local usage. Keep them unchanged unless you intentionally coordinate matching auth configuration across web and backend services.

## Tests
```bash
npm test
```

## Notes
- For full platform run, use root `docker compose up --build`.
- Dev login is off by default and can be enabled with `ENABLE_DEV_LOGIN=true` for local-only testing.
- `.env.example` also includes `NEXT_PUBLIC_ENABLE_DEV_LOGIN` for compatibility with UI-side toggles.
