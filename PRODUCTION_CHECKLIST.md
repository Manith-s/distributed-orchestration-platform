# Production Deployment Checklist

## Pre-Deployment

### Security
- [ ] All passwords changed from defaults
- [ ] JWT secret generated (min 256 bits) and set via environment variable
- [ ] All database passwords set via environment variables
- [ ] Redis password set via environment variable
- [ ] ClickHouse password set via environment variable
- [ ] Grafana admin password changed
- [ ] Default admin user password changed (`admin/admin123`)
- [ ] TLS/HTTPS configured for all public endpoints
- [ ] API rate limiting configured and tested
- [ ] CORS policies properly configured
- [ ] Swagger UI disabled or access-restricted
- [ ] Security headers configured
- [ ] Firewall rules configured
- [ ] Intrusion detection system set up

### Configuration
- [ ] `.env` file created from `.env.template`
- [ ] All environment variables validated
- [ ] Database connection strings verified
- [ ] Kafka bootstrap servers configured
- [ ] Redis connection tested
- [ ] ClickHouse connection tested
- [ ] Service discovery configured (if using)
- [ ] Log levels set appropriately (INFO/WARN for prod)
- [ ] Database indexes verified
- [ ] Connection pool sizes tuned for production load

### Infrastructure
- [ ] Docker images built and tagged
- [ ] Container registry set up
- [ ] Docker Compose tested in staging
- [ ] Health checks configured for all services
- [ ] Resource limits set (CPU, memory)
- [ ] Persistent volumes configured
- [ ] Backup strategy defined
- [ ] Network policies configured
- [ ] Load balancer configured
- [ ] Auto-scaling policies defined

### Monitoring & Observability
- [ ] Prometheus scrape targets verified
- [ ] Grafana dashboards imported
- [ ] Alert rules configured
- [ ] Alertmanager set up (email/Slack notifications)
- [ ] Log aggregation tested
- [ ] Metrics collection verified
- [ ] Application performance monitoring (APM) configured
- [ ] Uptime monitoring configured
- [ ] Error tracking service configured (e.g., Sentry)

### Testing
- [ ] All unit tests passing
- [ ] Integration tests passing
- [ ] End-to-end tests passing
- [ ] Load testing completed
- [ ] Stress testing completed
- [ ] Security audit/penetration testing completed
- [ ] Disaster recovery tested
- [ ] Failover testing completed
- [ ] Backup and restore tested

### Documentation
- [ ] README.md updated
- [ ] DEPLOYMENT.md reviewed
- [ ] SECURITY.md reviewed
- [ ] API documentation current
- [ ] Runbooks created for common operations
- [ ] Incident response plan documented
- [ ] Architecture diagrams updated
- [ ] Troubleshooting guide created

---

## Deployment Steps

### 1. Environment Setup
```bash
# Copy environment template
cp .env.template .env

# Edit .env with production values
nano .env

# Validate environment variables
source .env
echo $POSTGRES_PASSWORD  # Should not be default
echo $JWT_SECRET  # Should be long random string
```

### 2. Database Initialization
```bash
# Start PostgreSQL
docker-compose up -d postgres

# Wait for healthy
docker-compose ps postgres

# Verify tables created
docker exec -it orchestration-postgres psql -U $POSTGRES_USER -d $POSTGRES_DB -c "\dt"

# Change admin password
docker exec -it orchestration-postgres psql -U $POSTGRES_USER -d $POSTGRES_DB -c "
UPDATE users SET password = '\$2a\$10\$NEW_BCRYPT_HASH_HERE' WHERE username = 'admin';
"
```

### 3. Start Infrastructure Services
```bash
# Start all infrastructure
docker-compose up -d postgres redis kafka clickhouse zookeeper prometheus grafana

# Verify all healthy
docker-compose ps

# Check logs for errors
docker-compose logs -f
```

### 4. Deploy Application Services
```bash
# Build all services
mvn clean package -DskipTests

# Start orchestrator
cd orchestrator-service
java -jar target/orchestrator-service-1.0.0-SNAPSHOT.jar

# Start workers (separate terminals)
WORKER_ID=worker-1 SERVER_PORT=8081 java -jar worker-service/target/worker-service-1.0.0-SNAPSHOT.jar
WORKER_ID=worker-2 SERVER_PORT=8082 java -jar worker-service/target/worker-service-1.0.0-SNAPSHOT.jar
WORKER_ID=worker-3 SERVER_PORT=8083 java -jar worker-service/target/worker-service-1.0.0-SNAPSHOT.jar

# Start log aggregator
java -jar log-aggregator-service/target/log-aggregator-service-1.0.0-SNAPSHOT.jar

# Start query service
java -jar query-service/target/query-service-1.0.0-SNAPSHOT.jar

# Start API gateway
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

### 5. Verification
```bash
# Run infrastructure tests
bash scripts/test-infrastructure.sh

# Run end-to-end tests
bash scripts/test-e2e.sh

# Test authentication
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"NEW_PASSWORD"}'

# Test job submission
TOKEN="YOUR_JWT_TOKEN"
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Job","type":"EMAIL","payload":"{}"}'
```

---

## Post-Deployment

### Immediate (First Hour)
- [ ] All services running and healthy
- [ ] Health check endpoints responding
- [ ] Metrics being collected
- [ ] Logs being aggregated
- [ ] Authentication working
- [ ] Sample job submitted and processed
- [ ] Monitoring dashboards accessible
- [ ] Alerts firing correctly (test with deliberate failure)

### First Day
- [ ] Monitor error rates
- [ ] Check resource utilization (CPU, memory, disk)
- [ ] Review application logs
- [ ] Verify backup jobs running
- [ ] Test failover scenarios
- [ ] Review security logs
- [ ] Check API response times

### First Week
- [ ] Analyze performance metrics
- [ ] Review and tune auto-scaling policies
- [ ] Optimize database queries if needed
- [ ] Review and adjust alert thresholds
- [ ] Conduct security audit
- [ ] Review incident response procedures
- [ ] Gather user feedback

### Ongoing
- [ ] Weekly security patches
- [ ] Monthly dependency updates
- [ ] Quarterly disaster recovery drills
- [ ] Regular performance reviews
- [ ] Continuous security monitoring
- [ ] Regular backup verification

---

## Rollback Plan

### If Deployment Fails

1. **Stop new version**:
   ```bash
   docker-compose down
   ```

2. **Restore database** (if schema changed):
   ```bash
   psql -U $POSTGRES_USER -d $POSTGRES_DB < backup.sql
   ```

3. **Start previous version**:
   ```bash
   git checkout <previous-tag>
   docker-compose up -d
   ```

4. **Verify rollback**:
   ```bash
   bash scripts/test-e2e.sh
   ```

5. **Notify stakeholders**

---

## Monitoring Dashboards

### Key Metrics to Monitor

**Application Metrics**:
- Request rate (req/sec)
- Error rate (%)
- Response time (p50, p95, p99)
- Job submission rate
- Job completion rate
- Job failure rate
- Queue depth

**Infrastructure Metrics**:
- CPU utilization (%)
- Memory utilization (%)
- Disk I/O
- Network I/O
- PostgreSQL connections
- Redis connections
- Kafka lag

**Business Metrics**:
- Active users
- Jobs processed (hourly/daily)
- Average job duration
- Success rate
- Worker utilization

### Dashboard URLs

- Grafana: `http://your-domain:3000`
- Prometheus: `http://your-domain:9090`
- Kafka UI: `http://your-domain:9001`

---

## Alerts Configuration

### Critical Alerts (PagerDuty/Phone)

- Any service down > 2 minutes
- Error rate > 10%
- Database connection failure
- Kafka consumer lag > 10,000 messages
- Disk space < 10%

### Warning Alerts (Email/Slack)

- High job failure rate > 5%
- Response time p95 > 2 seconds
- Memory usage > 80%
- CPU usage > 80%
- Unusual login activity

---

## Backup Strategy

### What to Backup

- PostgreSQL database (jobs, users)
- ClickHouse data (logs, metrics)
- Configuration files (.env, application.yml)
- SSL certificates
- Encryption keys

### Backup Schedule

- **Hourly**: Incremental PostgreSQL backup
- **Daily**: Full PostgreSQL backup
- **Weekly**: Full ClickHouse backup
- **Monthly**: Archive old logs

### Backup Commands

```bash
# PostgreSQL backup
pg_dump -U $POSTGRES_USER -d $POSTGRES_DB > backup_$(date +%Y%m%d_%H%M%S).sql

# ClickHouse backup
clickhouse-backup create
clickhouse-backup upload <backup-name>

# Configuration backup
tar -czf config_backup_$(date +%Y%m%d).tar.gz .env monitoring/ scripts/
```

---

## Scaling Guide

### Horizontal Scaling

**Add More Workers**:
```bash
WORKER_ID=worker-4 SERVER_PORT=8084 java -jar worker-service.jar
```

**Add More Orchestrators** (behind load balancer):
```bash
SERVER_PORT=8081 java -jar orchestrator-service.jar
```

### Vertical Scaling

**Increase Memory**:
```bash
java -Xms2g -Xmx4g -jar orchestrator-service.jar
```

**Increase Database Connections**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
```

### Kafka Partitioning

Increase Kafka partitions for better parallelism:
```bash
kafka-topics --bootstrap-server localhost:9092 \
  --topic job.tasks \
  --alter --partitions 6
```

---

## Troubleshooting

### Common Issues

**Issue**: Services can't connect to database
**Solution**: Check `POSTGRES_HOST` and `POSTGRES_PASSWORD` environment variables

**Issue**: JWT authentication failing
**Solution**: Verify `JWT_SECRET` is set and same across all instances

**Issue**: High Kafka consumer lag
**Solution**: Add more worker instances or increase partition count

**Issue**: Out of memory errors
**Solution**: Increase JVM heap size or container memory limits

**Issue**: Slow queries
**Solution**: Check database indexes, run `ANALYZE` on PostgreSQL

---

## Security Incident Response

### Suspected Breach

1. **Isolate**: Disconnect affected systems
2. **Assess**: Review logs, identify scope
3. **Contain**: Rotate all secrets immediately
4. **Eradicate**: Patch vulnerabilities
5. **Recover**: Restore from clean backup
6. **Review**: Post-mortem and improvements

### Contact Information

- **Security Team**: security@your-company.com
- **On-Call Engineer**: +1-XXX-XXX-XXXX
- **Incident Commander**: name@your-company.com

---

## Success Criteria

Deployment is successful when:

- [ ] All health checks pass
- [ ] Zero 5xx errors for 1 hour
- [ ] Response time p95 < 500ms
- [ ] Job success rate > 95%
- [ ] No memory leaks detected
- [ ] All alerts functioning
- [ ] Backup job successful
- [ ] Security scan passes

---

## Sign-Off

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Tech Lead | | | |
| DevOps | | | |
| Security | | | |
| QA | | | |
| Product Owner | | | |

**Deployment Date**: _______________
**Deployment By**: _______________
**Approved By**: _______________
