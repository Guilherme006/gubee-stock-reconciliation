#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
USERNAME="${GUBEE_SECURITY_USER:-admin}"
PASSWORD="${GUBEE_SECURITY_PASSWORD:-gubee-admin}"
ACCOUNT_ID="${SMOKE_ACCOUNT_ID:-account-smoke}"
REST_SKU="${SMOKE_REST_SKU:-SKU-SMOKE-REST}"
KAFKA_SKU="${SMOKE_KAFKA_SKU:-SKU-SMOKE-KAFKA}"
EVENT_SUFFIX="$(date +%Y%m%d%H%M%S)"
REST_EVENT_ID="evt-smoke-rest-${EVENT_SUFFIX}"
KAFKA_EVENT_ID="evt-smoke-kafka-${EVENT_SUFFIX}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

request_status() {
  curl -s -o /tmp/gubee-smoke-response.json -w "%{http_code}" "$@"
}

expect_status() {
  local expected="$1"
  shift
  local status
  status="$(request_status "$@")"
  if [[ "$status" != "$expected" ]]; then
    echo "Expected HTTP $expected, got $status" >&2
    cat /tmp/gubee-smoke-response.json >&2 || true
    exit 1
  fi
}

wait_for_json_contains() {
  local url="$1"
  local expected="$2"
  local auth="${3:-}"

  for _ in {1..20}; do
    if [[ "$auth" == "auth" ]]; then
      curl -s -u "${USERNAME}:${PASSWORD}" "$url" > /tmp/gubee-smoke-response.json
    else
      curl -s "$url" > /tmp/gubee-smoke-response.json
    fi

    if grep -q "$expected" /tmp/gubee-smoke-response.json; then
      return 0
    fi

    sleep 1
  done

  echo "Timed out waiting for '$expected' at $url" >&2
  cat /tmp/gubee-smoke-response.json >&2 || true
  exit 1
}

require_command curl
require_command docker

echo "Checking public health endpoint..."
expect_status 200 "${BASE_URL}/actuator/health"

echo "Checking Swagger UI..."
expect_status 200 -L "${BASE_URL}/swagger-ui.html"

echo "Checking OpenAPI JSON..."
expect_status 200 "${BASE_URL}/v3/api-docs"

echo "Checking write endpoint authentication..."
expect_status 401 \
  -X POST "${BASE_URL}/api/v1/events" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"evt-smoke-unauth-${EVENT_SUFFIX}\",\"type\":\"STOCK_ADJUSTED\",\"occurredAt\":\"2026-05-28T10:00:00Z\",\"accountId\":\"${ACCOUNT_ID}\",\"sku\":\"SKU-SMOKE-UNAUTH\",\"available\":1,\"reason\":\"smoke\"}"

echo "Processing REST event..."
expect_status 202 \
  -u "${USERNAME}:${PASSWORD}" \
  -X POST "${BASE_URL}/api/v1/events" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"${REST_EVENT_ID}\",\"type\":\"STOCK_ADJUSTED\",\"occurredAt\":\"2026-05-28T10:00:00Z\",\"accountId\":\"${ACCOUNT_ID}\",\"sku\":\"${REST_SKU}\",\"available\":11,\"reason\":\"smoke_rest\"}"

wait_for_json_contains "${BASE_URL}/api/v1/stocks/${ACCOUNT_ID}/${REST_SKU}" '"available":11'
wait_for_json_contains "${BASE_URL}/api/v1/events/${REST_EVENT_ID}" '"status":"APPLIED"'

echo "Publishing Kafka event..."
printf '%s\n' "${ACCOUNT_ID}:${KAFKA_SKU}|{\"eventId\":\"${KAFKA_EVENT_ID}\",\"type\":\"STOCK_ADJUSTED\",\"occurredAt\":\"2026-05-28T10:05:00Z\",\"accountId\":\"${ACCOUNT_ID}\",\"sku\":\"${KAFKA_SKU}\",\"available\":7,\"reason\":\"smoke_kafka\"}" \
  | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
      --bootstrap-server localhost:9092 \
      --topic stock-events \
      --property parse.key=true \
      --property 'key.separator=|'

wait_for_json_contains "${BASE_URL}/api/v1/stocks/${ACCOUNT_ID}/${KAFKA_SKU}" '"available":7'
wait_for_json_contains "${BASE_URL}/api/v1/events/${KAFKA_EVENT_ID}" '"status":"APPLIED"'
wait_for_json_contains "${BASE_URL}/actuator/metrics/stock.events.processed" '"stock.events.processed"' auth

echo "Smoke test passed."
