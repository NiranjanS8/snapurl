#!/usr/bin/env bash

set -euo pipefail

APP_DIR="${1:-}"
BRANCH="${2:-main}"

if [[ -z "$APP_DIR" ]]; then
  echo "Missing app directory. Pass the EC2 repo path as the first argument."
  exit 1
fi

if [[ ! -d "$APP_DIR/.git" ]]; then
  echo "Git repository not found at: $APP_DIR"
  exit 1
fi

cd "$APP_DIR"

if [[ ! -f ".env.aws" ]]; then
  echo ".env.aws is missing in $APP_DIR"
  exit 1
fi

echo "Deploying branch: $BRANCH"
git fetch origin "$BRANCH"
git checkout "$BRANCH"
git pull --ff-only origin "$BRANCH"

echo "Rebuilding and restarting backend stack"
docker compose -f docker-compose.aws.yml --env-file .env.aws up -d --build

echo "Current containers:"
docker compose -f docker-compose.aws.yml ps

echo "Waiting for backend health check"
for attempt in {1..18}; do
  if curl --silent --fail http://127.0.0.1:9090/api/health >/dev/null; then
    echo "Backend health check passed"
    exit 0
  fi

  echo "Health check not ready yet (attempt $attempt/18)"
  sleep 5
done

echo "Backend health check failed"
docker compose -f docker-compose.aws.yml --env-file .env.aws logs app --tail 100
exit 1
