# Event Management backend

Spring Boot API. Published image: `ghcr.io/induwara90/event-management-uni-ruhuna:latest`.

## Local run (Compose)

```bash
docker compose up -d --build
```

- UI: http://localhost:8080
- API: http://localhost:8081
- MySQL: 127.0.0.1:3307 (database `events`)

Default login after first start: `ADMIN/001` / `1234`.

## Production / VM auto-deploy

See **[DEPLOY.md](./DEPLOY.md)** for Docker-based CI/CD:

`git push` → build & push GHCR image → SSH to VM → `docker compose pull && up`.

Deploy assets live in `deploy/`.
