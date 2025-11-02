#!/bin/bash

echo "=== Load Test: Submitting 100 Jobs ==="

ORCHESTRATOR="http://localhost:8080"
SUCCESS=0
FAILED=0

for i in {1..100}; do
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $ORCHESTRATOR/api/v1/jobs \
      -H "Content-Type: application/json" \
      -d "{
        \"name\": \"Load Test Job $i\",
        \"type\": \"EMAIL\",
        \"payload\": \"{}\",
        \"priority\": 1
      }")
    
    if [ "$RESPONSE" -eq 200 ] || [ "$RESPONSE" -eq 201 ]; then
        ((SUCCESS++))
        echo -n "."
    else
        ((FAILED++))
        echo -n "x"
    fi
    
    # Small delay to avoid overwhelming the system
    sleep 0.1
done

echo -e "\n"
echo "Results:"
echo "  Success: $SUCCESS"
echo "  Failed: $FAILED"
echo "  Success Rate: $(echo "scale=2; $SUCCESS * 100 / 100" | bc)%"

echo -e "\nWaiting 30 seconds for processing..."
sleep 30

echo -e "\nChecking final job stats..."
curl -s $ORCHESTRATOR/api/v1/jobs/stats

