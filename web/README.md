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
See `.env.example` for full list. Required for normal local flow:
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_ID`
- `AUCTOR_JWT_SECRET`
- `AUCTOR_JWT_ISSUER`
- `AUCTOR_JWT_AUDIENCE`
- `DEFINITION_SERVICE_URL`
- `EXECUTION_SERVICE_URL`

## Tests
```bash
npm test
```

## Notes
- For full platform run, use root `docker compose up --build`.
- `ENABLE_DEV_LOGIN` defaults to `false` unless explicitly set to `true`.
