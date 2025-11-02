#!/bin/bash

echo "=== End-to-End Integration Test ==="

# Base URLs
ORCHESTRATOR="http://localhost:8080"
QUERY="http://localhost:8085"
GATEWAY="http://localhost:8000"

# Test 0: Login and get JWT token
echo -e "\n0. Getting JWT token..."
TOKEN_RESPONSE=$(curl -s -X POST $ORCHESTRATOR/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

TOKEN=$(echo $TOKEN_RESPONSE | grep -oP '(?<="token":")[^"]*' || echo $TOKEN_RESPONSE | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
    echo "✗ Failed to get JWT token"
    echo "Response: $TOKEN_RESPONSE"
    exit 1
fi

echo "✓ JWT token obtained"

# Test 1: Submit Job (with authentication)
echo -e "\n1. Testing job submission..."
JOB_RESPONSE=$(curl -s -X POST $ORCHESTRATOR/api/v1/jobs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "E2E Test Job",
    "type": "EMAIL",
    "payload": "{\"to\":\"test@example.com\",\"subject\":\"Test\"}",
    "priority": 5
  }')

JOB_ID=$(echo $JOB_RESPONSE | grep -oP '(?<="id":")[^"]*' || echo $JOB_RESPONSE | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
echo "✓ Job created: $JOB_ID"

# Test 2: Get Job Status (with authentication)
echo -e "\n2. Testing job retrieval..."
curl -s -H "Authorization: Bearer $TOKEN" $ORCHESTRATOR/api/v1/jobs/$JOB_ID | grep -q "id"
if [ $? -eq 0 ]; then
    echo "✓ Job retrieved successfully"
else
    echo "✗ Failed to retrieve job"
fi

# Test 3: Wait for Processing
echo -e "\n3. Waiting for job processing (15 seconds)..."
sleep 15

# Test 4: Check Job Status
echo -e "\n4. Checking job completion..."
STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" $ORCHESTRATOR/api/v1/jobs/$JOB_ID | grep -oP '(?<="status":")[^"]*' || echo "UNKNOWN")
echo "Job status: $STATUS"

# Test 5: Search Logs (Query Service - Basic Auth)
echo -e "\n5. Testing log search..."
curl -s -u admin:admin123 -X POST $QUERY/api/v1/logs/search \
  -H "Content-Type: application/json" \
  -d '{"jobId":"'$JOB_ID'","limit":10}' | grep -q "logs"
if [ $? -eq 0 ]; then
    echo "✓ Logs found for job"
else
    echo "✗ No logs found"
fi

# Test 6: Get Metrics
echo -e "\n6. Testing metrics endpoint..."
curl -s -u admin:admin123 $QUERY/api/v1/metrics | grep -q "totalLogs"
if [ $? -eq 0 ]; then
    echo "✓ Metrics retrieved successfully"
else
    echo "✗ Failed to retrieve metrics"
fi

# Test 7: Gateway Routing (with authentication)
echo -e "\n7. Testing API Gateway routing..."
curl -s -H "Authorization: Bearer $TOKEN" $GATEWAY/api/v1/jobs/stats | grep -q "total"
if [ $? -eq 0 ]; then
    echo "✓ Gateway routing works"
else
    echo "✗ Gateway routing failed"
fi

echo -e "\n=== E2E Test Complete ==="

