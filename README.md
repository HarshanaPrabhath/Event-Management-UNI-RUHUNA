# Event Management backend

Spring Boot API for the university event system. The published image is `ghcr.io/induwara90/event-management-uni-ruhuna:latest`.

The frontend image is self-contained and can run with `docker run`. The backend cannot: it needs MySQL and the env vars below. Use Compose so MySQL, backend, and frontend start together.

## Run with GitHub images (no local build)

```bash
docker compose pull
docker compose up -d --no-build
```

- Frontend: http://localhost:8080
- Backend API: http://localhost:8081
- MySQL: localhost:3307 (database `events`)

Default login after first start: `ADMIN/001` / `1234`.

Copy `.env.example` to `.env` only if you need to change passwords or image names. Compose already has working defaults (`DB_PASSWORD=2003`).

## Rebuild locally

```bash
docker compose up -d --build
```
