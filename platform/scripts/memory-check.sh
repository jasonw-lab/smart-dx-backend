#!/bin/bash
# memory-check.sh - Check memory usage of Smart DX Backend containers

echo "=== Smart DX Backend Memory Usage ==="
echo "Date: $(date)"
echo ""

# Check if docker is available
if ! command -v docker &> /dev/null; then
    echo "Error: docker command not found"
    exit 1
fi

# Get container stats
echo "Container Memory Usage:"
echo "------------------------"
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}" | grep -E "smart-dx|NAME"

echo ""
echo "Individual Container Details:"
echo "-----------------------------"

for container in $(docker ps --filter "name=smart-dx" --format "{{.Names}}"); do
    echo ""
    echo "Container: $container"
    docker stats --no-stream --format "  Memory: {{.MemUsage}} ({{.MemPerc}})" "$container"
done

echo ""
echo "=== Total System Memory ==="
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    vm_stat | head -5
else
    # Linux
    free -h
fi
