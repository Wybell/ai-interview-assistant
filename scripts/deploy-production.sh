#!/usr/bin/env bash

set -euo pipefail

EXPECTED_SHA="${1:-}"
APP_DIR="/opt/ai-interview/app"
CONFIG_DIR="/opt/ai-interview/config"
ENV_FILE="$CONFIG_DIR/.env"
PROPERTIES_FILE="$CONFIG_DIR/application-prod.properties"
BACKUP_ROOT="$CONFIG_DIR/backups/database"
COMPOSE_FILE="$APP_DIR/backend/docker-compose.prod.yml"
COMPOSE=(docker compose -p backend --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

if [[ ! "$EXPECTED_SHA" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Expected a 40-character lowercase Git commit SHA." >&2
  exit 2
fi

deployment_failed() {
  local exit_code="$1"
  echo "Deployment failed. Current Compose status and recent logs follow." >&2
  "${COMPOSE[@]}" ps || true
  "${COMPOSE[@]}" logs --tail=200 backend frontend || true
  exit "$exit_code"
}

trap 'deployment_failed $?' ERR

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Required file is missing: $path" >&2
    exit 1
  fi
}

require_file "$ENV_FILE"
require_file "$PROPERTIES_FILE"
require_file "$COMPOSE_FILE"

if [[ ! -d "$BACKUP_ROOT" || ! -w "$BACKUP_ROOT" ]]; then
  echo "Deployment user must be able to write database backups to: $BACKUP_ROOT" >&2
  exit 1
fi

if [[ ! -d "$APP_DIR/.git" ]]; then
  echo "Application directory is not a Git checkout: $APP_DIR" >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required for production deployment." >&2
  exit 1
fi

cd "$APP_DIR"

if [[ "$(git symbolic-ref --quiet --short HEAD)" != "main" ]]; then
  echo "Production checkout must stay on the main branch." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Production checkout has uncommitted changes. Resolve them before deployment." >&2
  exit 1
fi

git fetch --quiet origin main
REMOTE_SHA="$(git rev-parse origin/main)"
if [[ "$REMOTE_SHA" != "$EXPECTED_SHA" ]]; then
  echo "GitHub workflow revision does not match origin/main; refusing deployment." >&2
  echo "Expected: $EXPECTED_SHA" >&2
  echo "Remote:   $REMOTE_SHA" >&2
  exit 1
fi

git merge --ff-only "$EXPECTED_SHA"

BACKUP_DIR="$BACKUP_ROOT/$(date +%Y%m%d-%H%M%S)"
install -d -m 700 "$BACKUP_DIR"
echo "Creating database backup in $BACKUP_DIR/interview_db.sql"
"${COMPOSE[@]}" exec -T mysql sh -ec \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --events "$MYSQL_DATABASE"' \
  > "$BACKUP_DIR/interview_db.sql"
chmod 600 "$BACKUP_DIR/interview_db.sql"

FRONTEND_HOST_PORT="$(awk -F= '$1 == "FRONTEND_HOST_PORT" { print $2; exit }' "$ENV_FILE" | tr -d '\r')"
if [[ ! "$FRONTEND_HOST_PORT" =~ ^[0-9]{1,5}$ ]]; then
  echo "FRONTEND_HOST_PORT must be set to a valid port in $ENV_FILE." >&2
  exit 1
fi

"${COMPOSE[@]}" config --quiet
"${COMPOSE[@]}" up -d --build --remove-orphans
"${COMPOSE[@]}" ps

backend_code=""
frontend_code=""
for _ in {1..30}; do
  backend_code="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:8082/api/ai/models || true)"
  frontend_code="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 5 "http://127.0.0.1:${FRONTEND_HOST_PORT}/" || true)"
  if [[ "$backend_code" == "401" && "$frontend_code" == "200" ]]; then
    break
  fi
  sleep 2
done

if [[ "$backend_code" != "401" ]]; then
  echo "Backend health check expected HTTP 401 but received: ${backend_code:-connection failure}" >&2
  exit 1
fi

if [[ "$frontend_code" != "200" ]]; then
  echo "Frontend health check expected HTTP 200 but received: ${frontend_code:-connection failure}" >&2
  exit 1
fi

echo "Deployment succeeded: $EXPECTED_SHA"
