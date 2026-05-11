#!/usr/bin/env bash

set -euo pipefail

APP_ROOT="${APP_ROOT:-/srv/echoim}"
APP_DIR="${APP_DIR:-$APP_ROOT/app/current}"
ENV_DIR="${ENV_DIR:-$APP_ROOT/env}"
SCRIPT_DIR="${SCRIPT_DIR:-$APP_ROOT/scripts}"
RELEASE_ENV_FILE="${RELEASE_ENV_FILE:-$ENV_DIR/release.env}"
APP_ENV_FILE="${APP_ENV_FILE:-$ENV_DIR/.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-$APP_DIR/deploy/compose/docker-compose.prod.yml}"
NGINX_CONF_SOURCE="${NGINX_CONF_SOURCE:-$APP_DIR/deploy/nginx/echoim.conf}"
NGINX_CONF_TARGET="${NGINX_CONF_TARGET:-/www/server/panel/vhost/nginx/echoim.conf}"
BUILD_ON_SERVER="${BUILD_ON_SERVER:-0}"

mkdir -p "$ENV_DIR" "$SCRIPT_DIR"

if [[ ! -f "$APP_ENV_FILE" ]]; then
  echo "missing app env file: $APP_ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "missing compose file: $COMPOSE_FILE" >&2
  exit 1
fi

if [[ -f "$RELEASE_ENV_FILE" ]]; then
  set -a
  source "$RELEASE_ENV_FILE"
  set +a
fi

export APP_SOURCE_DIR="${APP_SOURCE_DIR:-$APP_DIR}"
export BACKEND_SOURCE_DIR="${BACKEND_SOURCE_DIR:-$APP_DIR/backend/echoim-server}"
export APP_ENV_FILE

install -m 644 "$NGINX_CONF_SOURCE" "$NGINX_CONF_TARGET"
nginx -t
if systemctl is-active --quiet nginx 2>/dev/null; then
  systemctl reload nginx
else
  /www/server/nginx/sbin/nginx -c /www/server/nginx/conf/nginx.conf -s reload
fi

if [[ "$BUILD_ON_SERVER" == "1" ]]; then
  docker compose -f "$COMPOSE_FILE" build echoim-backend echoim-frontend
else
  docker compose -f "$COMPOSE_FILE" pull echoim-backend echoim-frontend || true
fi

docker compose -f "$COMPOSE_FILE" up -d --no-deps echoim-backend

for _ in {1..30}; do
  if curl -fsS http://127.0.0.1:18081/api/health >/dev/null; then
    break
  fi
  sleep 2
done

curl -fsS http://127.0.0.1:18081/api/health >/dev/null
docker compose -f "$COMPOSE_FILE" up -d --no-deps echoim-frontend
