# Performance Tuning Guide

## Database Optimization

### PostgreSQL
```sql
-- Increase connection pool
spring.datasource.hikari.maximum-pool-size=20

-- Add indexes
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_created_at ON jobs(created_at);
CREATE INDEX idx_jobs_priority ON jobs(priority DESC);
```

### ClickHouse
```sql
-- Optimize table
OPTIMIZE TABLE logs_db.job_logs FINAL;

-- Create materialized view for fast counts
CREATE MATERIALIZED VIEW logs_db.log_counts
ENGINE = SummingMergeTree()
ORDER BY (service_name, level)
AS SELECT
    service_name,
    level,
    count() as cnt
FROM logs_db.job_logs
GROUP BY service_name, level;
```

## Kafka Optimization

```yaml
# Increase partitions
kafka-topics.sh --alter --topic job.tasks --partitions 10

# Producer settings
spring.kafka.producer.batch-size=32768
spring.kafka.producer.linger.ms=10
spring.kafka.producer.compression-type=snappy

# Consumer settings
spring.kafka.consumer.max-poll-records=500
spring.kafka.consumer.fetch-min-bytes=1024
```

## JVM Tuning

```bash
# Orchestrator & Query Service
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Workers (lighter)
JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC"
```

## Redis Optimization

```
# Increase max memory
maxmemory 2gb
maxmemory-policy allkeys-lru

# Enable persistence
save 900 1
save 300 10
```

## Expected Performance

With proper tuning:
- **Throughput**: 10,000+ jobs/minute
- **Latency**: <100ms per job (p95)
- **Log ingestion**: 50,000+ logs/second
- **Query latency**: <200ms (log search)

## Application-Level Optimizations

### Orchestrator Service
```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 25
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
  
  kafka:
    producer:
      batch-size: 32768
      linger-ms: 10
      compression-type: snappy
```

### Worker Service
```yaml
# application-prod.yml
worker:
  pool-size: 10
  queue-capacity: 100
  
spring:
  kafka:
    consumer:
      max-poll-records: 500
      fetch-min-size: 1024
```

### Query Service
```yaml
# application-prod.yml
clickhouse:
  connection-pool-size: 20
  socket-timeout: 30000
  
spring:
  web:
    resources:
      cache:
        period: 3600
```

## Monitoring Performance

### Key Metrics to Watch

1. **Job Processing**
   - Job throughput (jobs/sec)
   - Job latency (p50, p95, p99)
   - Queue depth

2. **Database**
   - Connection pool utilization
   - Query execution time
   - Lock contention

3. **Kafka**
   - Consumer lag
   - Partition distribution
   - Broker CPU/disk

4. **System Resources**
   - CPU utilization
   - Memory usage
   - Network I/O

### Performance Testing

Run load tests regularly:
```bash
# Baseline test
bash scripts/load-test.sh

# Sustained load test (requires modification of script)
# Monitor for 10+ minutes, check for:
# - Memory leaks
# - CPU trends
# - Error rates
```

## Troubleshooting Slow Performance

### High Latency
1. Check database indexes
2. Review slow query logs
3. Monitor Kafka consumer lag
4. Check network latency between services

### Low Throughput
1. Scale workers horizontally
2. Increase Kafka partitions
3. Optimize database queries
4. Review application logs for bottlenecks

### Memory Issues
1. Increase JVM heap size
2. Review memory leaks using profiler
3. Adjust GC settings
4. Monitor object allocation rates

## Capacity Planning

### Recommended Resources

**Small Deployment (< 1000 jobs/minute):**
- 2 Workers: 1 CPU, 1GB RAM each
- 1 Orchestrator: 2 CPU, 2GB RAM
- 1 Query Service: 1 CPU, 1GB RAM

**Medium Deployment (1000-5000 jobs/minute):**
- 5 Workers: 2 CPU, 2GB RAM each
- 2 Orchestrators: 4 CPU, 4GB RAM each
- 2 Query Services: 2 CPU, 2GB RAM each

**Large Deployment (5000+ jobs/minute):**
- 10+ Workers: 4 CPU, 4GB RAM each
- 3+ Orchestrators: 8 CPU, 8GB RAM each
- 3+ Query Services: 4 CPU, 4GB RAM each

