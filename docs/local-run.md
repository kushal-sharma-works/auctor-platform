# Local Recruiter Demo (No Azure Cost)

Use this guide to run the Auctor local demo stack on your machine without cloud billing.

## Prerequisites
- Git
- Docker Desktop (or Docker Engine)
- Docker Compose plugin (`docker compose`)
- Recommended host capacity: 8 GB RAM, 4 CPU cores, 10 GB free disk
- Ports free: `3000`, `8081`, `8082`, `5432`, `6379`, `9091`, `3001`, `16686`

## First-time Machine Setup
Verify tooling:

```bash
git --version
docker --version
docker compose version
```

If `docker compose` is not available, install/enable the Compose v2 plugin before continuing.

Clone and enter repo:

```bash
git clone https://github.com/kushal-sharma-works/auctor-platform.git
cd auctor-platform
```

## Fastest Start
From repo root:

```bash
docker compose up --build
```

Open:
- Web: http://localhost:3000
- Definition service: http://localhost:8081/actuator/health
- Execution service: http://localhost:8082/health
- Grafana: http://localhost:3001 (admin/admin, Auctor dashboard opens by default)
- Jaeger: http://localhost:16686

## Shutdown
```bash
docker compose down -v
```

## Recruiter Demo Flow (5 minutes)
1. Open `http://localhost:3000`
2. Login with Google/dev flow as configured
3. Show workflow definitions and policies
4. Start an execution
5. Advance execution and show audit trail
6. Show observability in Grafana/Jaeger

## Troubleshooting
- If a port is busy:
  - `lsof -i :3000` (replace port) and stop conflicting process
- If containers fail:
  - `docker compose ps`
  - `docker compose logs --tail=100`
- If web not loading:
  - `docker compose restart web`
