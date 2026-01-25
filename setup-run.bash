#!/usr/bin/env bash

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$ROOT_DIR/infra"
DATA_DIR="$INFRA_DIR/data"

# Load .env
if [ -f "$ROOT_DIR/.env" ]; then
    echo "> Loading root .env"
    set -o allexport
    source "$ROOT_DIR/.env"
    set +o allexport
fi

echo "===[ finditnow: setup + run (Linux) ]==="

# Persistent directories
echo "> Ensuring persistent directories"
mkdir -p "$DATA_DIR/postgres" "$DATA_DIR/redis"

# Try fixing Postgres permissions (Linux only)
if command -v sudo >/dev/null 2>&1; then
   sudo chown -R 999:999 "$DATA_DIR/postgres" 2>/dev/null || true
fi

# Start infra
echo "> Starting infra containers"
cd "$INFRA_DIR"
docker compose up -d

# Wait for Postgres
echo "> Waiting for Postgres..."
ATTEMPTS=20
until docker exec "$(docker compose ps -q postgres)" pg_isready -U "${DB_USER:-devuser}" >/dev/null 2>&1; do
    ((ATTEMPTS--))
    [ "$ATTEMPTS" -le 0 ] && echo "!! Postgres startup timeout" && exit 1
    sleep 1
done
echo "> Postgres ready"

# Wait for Redis
echo "> Waiting for Redis..."
ATTEMPTS=20
until docker exec "$(docker compose ps -q redis)" redis-cli ping >/dev/null 2>&1; do
    ((ATTEMPTS--))
    [ "$ATTEMPTS" -le 0 ] && echo "!! Redis startup timeout" && exit 1
    sleep 1
done
echo "> Redis ready"

# --- RUN SERVICES (Linux style, background jobs) ---
echo "> Starting microservices..."

cd "$ROOT_DIR"

./gradlew :services:auth:run &
PID_AUTH=$!

./gradlew :services:user-service:bootRun &
PID_USER=$!

./gradlew :services:shop-service:bootRun &
PID_SHOP=$!

./gradlew :services:order-service:bootRun &
PID_ORDER=$!

./gradlew :services:delivery-service:bootRun &
PID_DELIVERY=$!

./gradlew :services:file-gateway:bootRun &
PID_FILE=$!

echo "> Started auth-service (PID: $PID_AUTH)"
echo "> Started user-service (PID: $PID_USER)"
echo "> Started shop-service (PID: $PID_SHOP)"
echo "> Started order-service (PID: $PID_ORDER)"
echo "> Started delivery-service (PID: $PID_DELIVERY)"
echo "> Started file-gateway (PID: $PID_FILE)"

echo
echo "===[ Everything running ]==="
echo "CTRL+C will stop only this script, not the services."
echo "Kill PIDs manually or use ./gradlew --stop"