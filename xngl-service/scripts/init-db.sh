#!/bin/sh
# 创建 xngl 库并执行 baseline + patches
# 使用: MYSQL_PASSWORD=yourpass ./scripts/init-db.sh  或 export MYSQL_PASSWORD 后执行

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SCHEMA_DIR="${SCRIPT_DIR}/../xngl-service-starter/src/main/resources/db/schema"

if [ -z "$MYSQL_PASSWORD" ]; then
  echo "Error: MYSQL_PASSWORD not set. Usage: MYSQL_PASSWORD=xxx $0"
  exit 1
fi

MYSQL_USERNAME="${MYSQL_USERNAME:-root}"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DATABASE:-xngl}"

MYSQL_CMD="mysql --default-character-set=utf8mb4 -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USERNAME -p$MYSQL_PASSWORD"

echo "Creating database $MYSQL_DB..."
$MYSQL_CMD -e "CREATE DATABASE IF NOT EXISTS $MYSQL_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "Running baseline.sql..."
$MYSQL_CMD "$MYSQL_DB" < "$SCHEMA_DIR/baseline.sql"

echo "Running patches..."
for f in "$SCHEMA_DIR/patches/"*.sql; do
  [ -f "$f" ] || continue
  file_name="$(basename "$f")"
  if [ "$file_name" = "001_vehicle_gps_optional.sql" ]; then
    echo "  $file_name (skip: large-table optional patch, apply manually if needed)"
    continue
  fi
  echo "  $file_name"
  $MYSQL_CMD "$MYSQL_DB" < "$f"
done

echo "Done."
