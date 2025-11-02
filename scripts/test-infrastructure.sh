#!/bin/bash

echo "=== Infrastructure Health Check ==="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

check_service() {
    local name=$1
    local port=$2
    
    if command -v nc >/dev/null 2>&1; then
        if nc -z localhost $port 2>/dev/null; then
            echo -e "${GREEN}✓${NC} $name (port $port) - UP"
            return 0
        else
            echo -e "${RED}✗${NC} $name (port $port) - DOWN"
            return 1
        fi
    else
        # Windows fallback using PowerShell
        if powershell -Command "Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet" 2>/dev/null | grep -q "True"; then
            echo -e "${GREEN}✓${NC} $name (port $port) - UP"
            return 0
        else
            echo -e "${RED}✗${NC} $name (port $port) - DOWN"
            return 1
        fi
    fi
}

echo "Checking Docker services..."
docker ps --format "table {{.Names}}\t{{.Status}}" | grep orchestration

echo -e "\nChecking service ports..."
check_service "PostgreSQL" 5432
check_service "Redis" 6379
check_service "Kafka" 9092
check_service "ClickHouse" 8123
check_service "Prometheus" 9090
check_service "Grafana" 3000

echo -e "\nChecking application services..."
check_service "Orchestrator" 8080
check_service "Query Service" 8085
check_service "Log Aggregator" 8084
check_service "API Gateway" 8000

echo -e "\n=== Health Check Complete ==="

