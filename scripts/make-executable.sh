#!/bin/bash

echo "Making scripts executable..."

chmod +x scripts/test-infrastructure.sh
chmod +x scripts/test-e2e.sh
chmod +x scripts/load-test.sh
chmod +x scripts/start-all.sh 2>/dev/null || true
chmod +x scripts/stop-all.sh 2>/dev/null || true

echo "✓ All scripts are now executable"
echo ""
echo "Available test scripts:"
echo "  ./scripts/test-infrastructure.sh  - Check infrastructure health"
echo "  ./scripts/test-e2e.sh             - Run end-to-end integration tests"
echo "  ./scripts/load-test.sh            - Run load tests"

