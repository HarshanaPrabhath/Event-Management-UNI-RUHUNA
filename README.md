# Event Management Backend

Spring Boot REST API for University Event Management System.

- **Published Image:** `ghcr.io/induwara90/event-management-uni-ruhuna:latest`
- **Default Port:** `8081`

## Local Development

To run with MySQL via Docker:
```bash
docker compose up -d
```

- API: `http://localhost:8081`
- MySQL: `127.0.0.1:3307` (database `events`)
- Default Admin Login: `ADMIN/001` / `1234`

## Automated VM Deployment

CI/CD is automated using GitHub Actions (`.github/workflows/docker-publish.yml`):
1. On `git push origin main`, GitHub Actions builds the Docker image and publishes it to GitHub Container Registry (GHCR).
2. It automatically SSHs into the production VM and pulls & updates the containers.
