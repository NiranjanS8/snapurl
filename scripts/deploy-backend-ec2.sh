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
