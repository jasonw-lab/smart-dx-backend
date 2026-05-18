#!/bin/bash
# start-all.sh - Start all Smart DX Backend services

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Starting Smart DX Backend Services ==="
echo "Platform directory: $PLATFORM_DIR"

cd "$PLATFORM_DIR"

# Start middleware first
echo ""
echo "Starting middleware (mysql, redis, opensearch)..."
docker-compose up -d mysql redis opensearch

# Wait for MySQL to be ready
echo ""
echo "Waiting for MySQL to be ready..."
until docker-compose exec -T mysql mysqladmin ping -h localhost --silent; do
    echo "  Waiting..."
    sleep 2
done
echo "MySQL is ready!"

# Wait for Redis to be ready
echo ""
echo "Waiting for Redis to be ready..."
until docker-compose exec -T redis redis-cli ping | grep -q PONG; do
    echo "  Waiting..."
    sleep 2
done
echo "Redis is ready!"

# Start nginx
echo ""
echo "Starting nginx..."
docker-compose up -d nginx

# Show status
echo ""
echo "=== Service Status ==="
docker-compose ps

echo ""
echo "All services started successfully!"
echo ""
echo "Endpoints:"
echo "  - MySQL:      localhost:3306"
echo "  - Redis:      localhost:6379"
echo "  - OpenSearch: localhost:9200"
echo "  - Nginx:      localhost:80"
