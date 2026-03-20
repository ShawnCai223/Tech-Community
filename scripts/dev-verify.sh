#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

echo "Running Elasticsearch integration verification..."
exec mvn -q \
  -Dcommunity.es.integration=true \
  -Dcommunity.es.ik.integration=true \
  -Dtest=ElasticsearchIntegrationTests \
  test
