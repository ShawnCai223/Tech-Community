#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/docker-compose.es-local.yml"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

wait_for_http() {
  local url="$1"
  local name="$2"
  local attempt
  for attempt in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "$name is ready"
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for $name at $url" >&2
  return 1
}

wait_for_redis() {
  local attempt
  for attempt in $(seq 1 60); do
    if redis-cli -p 6379 ping >/dev/null 2>&1; then
      echo "Redis is ready"
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for Redis on localhost:6379" >&2
  return 1
}

wait_for_mysql() {
  local attempt
  for attempt in $(seq 1 60); do
    if docker exec mysql-community mysql -uroot -pMQKY7CJx -e "SELECT 1" >/dev/null 2>&1; then
      echo "MySQL is ready"
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for MySQL in mysql-community" >&2
  return 1
}

require_cmd docker
require_cmd curl
require_cmd redis-cli
require_cmd mvn

cd "$ROOT_DIR"

echo "Starting local dependency stack..."
docker compose -f "$COMPOSE_FILE" up -d --build

echo "Waiting for local dependencies..."
wait_for_mysql
wait_for_redis
wait_for_http "http://localhost:9200/_cluster/health" "Elasticsearch"

echo "Seeding realistic like counts..."
"${ROOT_DIR}/scripts/dev-seed-likes.sh"

echo "Starting application with dev,es-local profiles..."
exec mvn spring-boot:run -Dspring-boot.run.profiles=dev,es-local
