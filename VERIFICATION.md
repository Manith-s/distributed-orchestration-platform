# System Verification Checklist

## Pre-deployment Checks

### Infrastructure
- [ ] Docker containers running
- [ ] PostgreSQL accessible (port 5432)
- [ ] Redis accessible (port 6379)
- [ ] Kafka accessible (port 9092)
- [ ] ClickHouse accessible (port 8123)

### Services
- [ ] Orchestrator started (port 8080)
- [ ] 3+ Workers started (ports 8081-8083)
- [ ] Log Aggregator started (port 8084)
- [ ] Query Service started (port 8085)
- [ ] API Gateway started (port 8000)

### Health Checks
```bash
curl http://localhost:8080/actuator/health  # Should return UP
curl http://localhost:8085/actuator/health  # Should return UP
curl http://localhost:8084/health           # Should return UP
```

## Functional Tests

### Job Submission
```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","type":"EMAIL","payload":"{}","priority":1}'
# Expected: 200 OK with job ID
```

### Job Execution
- [ ] Job status changes from PENDING → QUEUED → RUNNING → COMPLETED
- [ ] Worker logs show job execution
- [ ] Results sent back to orchestrator

### Log Aggregation
- [ ] Logs appear in ClickHouse within 5 seconds
- [ ] Can search logs via Query Service
- [ ] Log counts match submission counts

### Metrics
- [ ] Prometheus scraping all targets
- [ ] Grafana dashboards showing data
- [ ] Alerts configured properly

## Performance Tests

### Load Test
```bash
bash scripts/load-test.sh
# Expected: >95% success rate
```

### Sustained Load
- [ ] System handles 100+ jobs/minute for 10 minutes
- [ ] No memory leaks (check with `jconsole`)
- [ ] CPU usage stays <80%

## Monitoring Verification

- [ ] All services showing UP in Grafana
- [ ] Job metrics updating in real-time
- [ ] Error alerts not firing (unless there are actual errors)
- [ ] Log volume tracking correctly

## Security Checks

- [ ] No default passwords in production
- [ ] Services not exposed to public internet
- [ ] API rate limiting working
- [ ] CORS configured properly

## Documentation

- [ ] README.md complete
- [ ] DEPLOYMENT.md reviewed
- [ ] API documentation accessible
- [ ] Architecture diagrams up to date

## Automated Tests

### Run Infrastructure Test
```bash
bash scripts/test-infrastructure.sh
```

Expected output:
- All Docker services running
- All ports accessible
- Green checkmarks for all services

### Run E2E Test
```bash
bash scripts/test-e2e.sh
```

Expected output:
- Job submission successful
- Job retrieved successfully
- Job processing complete
- Logs found
- Metrics retrieved
- Gateway routing works

### Run Load Test
```bash
bash scripts/load-test.sh
```

Expected output:
- Success rate > 95%
- All jobs complete within 60 seconds
- No errors in service logs

## Database Verification

### PostgreSQL
```bash
docker exec -it orchestration-postgres psql -U postgres -d jobs_db -c "SELECT COUNT(*) FROM jobs;"
```

Expected: Job count matches submissions

### ClickHouse
```bash
docker exec -it orchestration-clickhouse clickhouse-client --query "SELECT COUNT(*) FROM logs_db.job_logs"
```

Expected: Log count > 0

## Kafka Verification

```bash
docker exec -it orchestration-kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

Expected topics:
- job.tasks
- job.results
- job.logs

## API Documentation Verification

### Swagger UI Access
- [ ] Orchestrator Swagger: http://localhost:8080/swagger-ui.html
- [ ] Query Service Swagger: http://localhost:8085/swagger-ui.html

### API Endpoints Test
```bash
# List all jobs
curl http://localhost:8080/api/v1/jobs

# Get job stats
curl http://localhost:8080/api/v1/jobs/stats

# Search logs
curl -X POST http://localhost:8085/api/v1/logs/search \
  -H "Content-Type: application/json" \
  -d '{"limit":10}'

# Get metrics
curl http://localhost:8085/api/v1/metrics
```

## Monitoring Stack Verification

### Prometheus
```bash
# Check targets
curl http://localhost:9090/api/v1/targets
```

Expected: All targets UP

### Grafana
- [ ] Login successful (admin/admin)
- [ ] Dashboards loaded
- [ ] Data sources connected
- [ ] Panels showing data

## Error Scenarios

### Test Job Failure Handling
```bash
# Submit invalid job
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"name":"","type":"INVALID","payload":"{}"}'
# Expected: 400 Bad Request
```

### Test Service Resilience
1. Stop one worker: `docker stop worker-1`
2. Submit jobs: Should continue processing
3. Restart worker: `docker start worker-1`
4. Verify: Worker rejoins and processes jobs

## Final Checklist

Before going to production:
- [ ] All automated tests pass
- [ ] Load test shows acceptable performance
- [ ] No errors in any service logs
- [ ] Monitoring dashboards working
- [ ] API documentation accessible
- [ ] Backup strategy implemented
- [ ] Security hardening complete
- [ ] Deployment documentation reviewed
- [ ] Rollback plan documented
- [ ] On-call team trained

## Post-Deployment Verification

After deploying to production:
1. Run smoke tests immediately
2. Monitor for 15 minutes
3. Check error rates
4. Verify metrics collection
5. Test critical user flows
6. Review logs for warnings

## Troubleshooting Common Issues

### Issue: Jobs stuck in PENDING
**Check:**
- Kafka connectivity
- Worker availability
- Database locks

### Issue: Logs not appearing
**Check:**
- Log Aggregator running
- Kafka topic exists
- ClickHouse connection

### Issue: High latency
**Check:**
- Database connection pool
- Kafka consumer lag
- Network latency
- Resource constraints

## Success Criteria

System is production-ready when:
- ✅ All health checks pass
- ✅ E2E test passes
- ✅ Load test shows >95% success rate
- ✅ No errors in logs
- ✅ Monitoring working
- ✅ Documentation complete
- ✅ Security checklist complete

