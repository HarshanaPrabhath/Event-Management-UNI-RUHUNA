#!/usr/bin/env bash
# Idempotent deploy used by GitHub Actions over SSH.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/event-management}"
cd "$DEPLOY_DIR"

if [[ ! -f .env ]]; then
  echo "ERROR: $DEPLOY_DIR/.env is missing. Copy .env.example and set secrets first."
  exit 1
fi

# Optional: log in to GHCR when packages are private.
# Create a classic PAT with read:packages and put it in .env as GHCR_TOKEN + GHCR_USER.
if [[ -n "${GHCR_TOKEN:-}" && -n "${GHCR_USER:-}" ]]; then
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
elif grep -q '^GHCR_TOKEN=' .env 2>/dev/null; then
  # shellcheck disable=SC1091
  set -a
  # shellcheck source=/dev/null
  source .env
  set +a
  if [[ -n "${GHCR_TOKEN:-}" && -n "${GHCR_USER:-}" ]]; then
    echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
  fi
fi

echo "Pulling images..."
docker compose pull

echo "Starting stack..."
docker compose up -d --remove-orphans

# Load ports from .env for health checks
set -a
# shellcheck source=/dev/null
source .env
set +a

echo "Waiting for healthy services..."
for i in $(seq 1 40); do
  if curl -fsS "http://127.0.0.1:${FRONTEND_PORT:-8080}/" >/dev/null 2>&1 \
     && curl -fsS "http://127.0.0.1:${BACKEND_PORT:-8081}/api/check" >/dev/null 2>&1; then
    echo "Deploy healthy."
    docker compose ps
    exit 0
  fi
  sleep 3
done

echo "Health check timed out."
docker compose ps
docker compose logs --tail=80
exit 1
