# Distributed Orchestration Platform

A production-grade **distributed job orchestration engine** with integrated **log aggregation** and **real-time monitoring**. Built with Java, Spring Boot, and modern distributed systems technologies.

## Overview

This platform combines three critical capabilities:

1. **Job Orchestration Engine** - Distributed task execution across multiple workers
2. **Log Aggregation System** - Centralized logging with real-time search
3. **Monitoring & Observability** - Metrics, dashboards, and alerting

### Key Features

- Submit jobs via REST API with JSON payloads
- Execute jobs across multiple worker nodes with automatic load balancing
- Automatic retry with exponential backoff for failed jobs
- Distributed locking (Redis) to prevent duplicate processing
- Aggregate logs from all services in real-time
- Full-text search on logs with sub-second latency
- Real-time metrics dashboards (Grafana)
- Handle 10,000+ jobs per minute
- ACID-compliant job state management (PostgreSQL)
- Time-series optimized log storage (ClickHouse)
- **JWT-based authentication** with role-based access control (RBAC)
- **Production-ready security** with encrypted passwords and environment-based secrets

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          API Gateway                                 │
│                     (Rate Limiting, Routing)                         │
└────────────────────────────┬────────────────────────────────────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐
│   Orchestrator   │ │    Query     │ │  Log Aggregator  │
│     Service      │ │   Service    │ │     Service      │
└────────┬─────────┘ └──────┬───────┘ └────────┬─────────┘
         │                  │                   │
         │                  │                   │
    ┌────▼────┐        ┌────▼─────┐       ┌────▼────┐
    │  Kafka  │        │ClickHouse│       │  Kafka  │
    └────┬────┘        └──────────┘       └─────────┘
         │
    ┌────▼────────────────────┐
    │   Worker Service        │
    │   (3+ Instances)        │
    └─────────────────────────┘
         │
    ┌────▼────┐
    │PostgreSQL│
    │  Redis  │
    └─────────┘

         │
    ┌────▼────────┐
    │ Prometheus  │
    │   Grafana   │
    └─────────────┘
```

## Technology Stack

### Core Services (Java + Spring Boot)
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.5
- **Build Tool**: Maven (multi-module project)

### Services
1. **orchestrator-service** (Port 8080) - Job submission, scheduling, and distribution
2. **worker-service** (Ports 8081-8083) - Job execution workers (3+ instances)
3. **log-aggregator-service** (Port 8084) - Log ingestion and storage
4. **query-service** (Port 8085) - Log search and metrics API
5. **api-gateway** (Port 8086) - Spring Cloud Gateway

### Infrastructure
- **PostgreSQL 15** - Job state and metadata (ACID compliance)
- **ClickHouse** - Logs and metrics storage (time-series optimized)
- **Apache Kafka** - Message queue for jobs and log streaming
- **Redis 7** - Distributed locks and caching
- **Prometheus** - Metrics collection
- **Grafana** - Visualization and dashboards

## Project Structure

```
distributed-orchestration-platform/
├── pom.xml                          # Parent POM
├── docker-compose.yml               # Infrastructure services
├── README.md
├── .gitignore
│
├── orchestrator-common/             # Shared models and utilities
│   ├── pom.xml
│   └── src/main/java/com/platform/common/
│       ├── model/
│       │   ├── Job.java             # Job entity
│       │   ├── JobStatus.java       # Status enum
│       │   └── LogEntry.java        # Log model
│       └── config/
│
├── orchestrator-service/            # Core orchestration service
│   ├── pom.xml
│   └── src/main/java/com/platform/orchestrator/
│
├── worker-service/                  # Job execution workers
│   ├── pom.xml
│   └── src/main/java/com/platform/worker/
│
├── log-aggregator-service/          # Log ingestion
│   ├── pom.xml
│   └── src/main/java/com/platform/logaggregator/
│
├── query-service/                   # Search and metrics API
│   ├── pom.xml
│   └── src/main/java/com/platform/query/
│
├── api-gateway/                     # API Gateway
│   ├── pom.xml
│   └── src/main/java/com/platform/gateway/
│
├── scripts/                         # Database initialization
│   ├── postgres-init.sql
│   └── clickhouse-init.sql
│
└── monitoring/                      # Monitoring configs
    └── prometheus.yml
```

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **Docker Desktop** ([Download](https://www.docker.com/products/docker-desktop))
- **Docker Compose** (included with Docker Desktop)

Verify installations:

```bash
java -version    # Should show Java 17+
mvn -version     # Should show Maven 3.8+
docker --version
docker-compose --version
```

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd distributed-orchestration-platform
```

### 2. Configure Environment Variables

Copy the environment template and configure your secrets:

```bash
cp .env.example .env
```

Edit `.env` and change all default passwords:
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `CLICKHOUSE_PASSWORD`
- `GRAFANA_ADMIN_PASSWORD`
- `JWT_SECRET` (use at least 256 bits)

**For development**, you can use the default values. **For production**, change all passwords!

### 3. Start Infrastructure Services

Start all infrastructure services (PostgreSQL, Kafka, Redis, ClickHouse, Prometheus, Grafana):

```bash
# Load environment variables
export $(cat .env | xargs)

# Start infrastructure
docker-compose up -d
```

Verify all services are running:

```bash
docker-compose ps
```

All services should show "healthy" status after ~30 seconds.

### 4. Build the Project

```bash
mvn clean install
```

This will:
- Compile all modules
- Run unit tests
- Package each service as a JAR

### 5. Run Services

```bash
# Terminal 1 - Orchestrator Service
java -jar orchestrator-service/target/orchestrator-service-1.0.0-SNAPSHOT.jar

# Terminal 2 - Worker Service (Instance 1)
java -jar worker-service/target/worker-service-1.0.0-SNAPSHOT.jar --server.port=8081

# Terminal 3 - Worker Service (Instance 2)
java -jar worker-service/target/worker-service-1.0.0-SNAPSHOT.jar --server.port=8082

# Terminal 4 - Log Aggregator
java -jar log-aggregator-service/target/log-aggregator-service-1.0.0-SNAPSHOT.jar

# Terminal 5 - Query Service
java -jar query-service/target/query-service-1.0.0-SNAPSHOT.jar

# Terminal 6 - API Gateway
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

## Infrastructure Access

Once `docker-compose up -d` is running, you can access:

| Service | URL | Credentials |
|---------|-----|-------------|
| **Orchestrator API** | http://localhost:8080 | JWT token required (see below) |
| **Query Service API** | http://localhost:8085 | admin / admin123 (Basic Auth) |
| **Kafka UI** | http://localhost:9001 | - |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | admin / admin (change in production) |
| **PostgreSQL** | localhost:5432 | postgres / postgres (change in production) |
| **Redis** | localhost:6379 | Password: redis_password |
| **ClickHouse** | localhost:8123 (HTTP)<br>localhost:9000 (Native) | default / (empty) |

### Authentication

The platform uses **JWT authentication** for the Orchestrator service and **Basic Auth** for the Query service.

**Default credentials (development only):**
- **Username**: `admin`
- **Password**: `admin123`

⚠️ **Change these credentials in production!** See [SECURITY.md](SECURITY.md) for details.

### Database Connections

**PostgreSQL:**
```bash
docker exec -it orchestration-postgres psql -U admin -d orchestration_db
```

**ClickHouse:**
```bash
docker exec -it orchestration-clickhouse clickhouse-client
```

**Redis:**
```bash
docker exec -it orchestration-redis redis-cli -a redis_password
```

## Job Lifecycle

Jobs progress through the following states:

```
PENDING → QUEUED → RUNNING → COMPLETED
                         ↓
                      FAILED → RETRYING → RUNNING
                         ↓
                    DEAD_LETTER (max retries exceeded)
```

### Job States

- **PENDING**: Job created but not yet queued
- **QUEUED**: Job sent to Kafka, waiting for worker
- **RUNNING**: Job being processed by a worker
- **COMPLETED**: Job finished successfully
- **FAILED**: Job failed but has retries remaining
- **RETRYING**: Job is being retried
- **DEAD_LETTER**: Permanently failed (manual intervention required)

## API Usage

### Authentication

First, obtain a JWT token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

This returns:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "admin"
}
```

### Submit a Job

Use the JWT token in the `Authorization` header:

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Send Welcome Email",
    "type": "EMAIL",
    "payload": "{\"to\":\"user@example.com\",\"template\":\"welcome\"}",
    "priority": 5
  }'
```

### Get Job Status

```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/v1/jobs/{job-id}
```

### Search Logs

The Query Service uses Basic Authentication:

```bash
curl -u admin:admin123 -X POST http://localhost:8085/api/v1/logs/search \
  -H "Content-Type: application/json" \
  -d '{
    "jobId": "your-job-id",
    "limit": 10
  }'
```

For complete security documentation, see [SECURITY.md](SECURITY.md).

## Monitoring

### Prometheus Metrics

Access Prometheus at http://localhost:9090

**Key Metrics:**
- `jobs_total` - Total jobs processed
- `jobs_duration_seconds` - Job execution duration
- `jobs_failed_total` - Failed job count
- `kafka_consumer_lag` - Message queue lag
- `http_requests_total` - API request count

### Grafana Dashboards

Access Grafana at http://localhost:3000 (admin/admin)

**Pre-configured Dashboards** (Phase 3):
- Job Throughput & Latency
- Error Rates by Service
- Worker Performance
- Kafka Consumer Lag
- System Resources

## Development

### Build a Single Module

```bash
cd orchestrator-service
mvn clean install
```

### Run Tests

```bash
# All modules
mvn test

# Specific module
mvn test -pl orchestrator-service
```

### Hot Reload with Spring DevTools

Add to your application.yml:

```yaml
spring:
  devtools:
    restart:
      enabled: true
```

## Configuration

Each service uses `application.yml` for configuration. Key properties:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orchestration_db
    username: admin
    password: admin_password

  kafka:
    bootstrap-servers: localhost:9092

  redis:
    host: localhost
    port: 6379
    password: redis_password
```

## Performance Tuning

### Kafka Partitions

Adjust partitions for higher throughput:

```bash
docker exec -it orchestration-kafka kafka-topics \
  --alter --topic jobs --partitions 10 \
  --bootstrap-server localhost:9092
```

### Worker Scaling

Run additional worker instances:

```bash
java -jar worker-service/target/worker-service-1.0.0-SNAPSHOT.jar --server.port=8084
```

### Database Indexing

PostgreSQL indexes are created automatically via `scripts/postgres-init.sql`.

## Documentation

This project includes comprehensive documentation for production deployment:

- **[SECURITY.md](SECURITY.md)** - Authentication, authorization, and security best practices
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Complete deployment guide with step-by-step instructions
- **[PERFORMANCE.md](PERFORMANCE.md)** - Performance tuning and optimization guide
- **[VERIFICATION.md](VERIFICATION.md)** - System verification checklist and testing procedures
- **[PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md)** - Pre-deployment production checklist

## Testing

### Automated Test Scripts

Run the test suite to verify your deployment:

```bash
# Check infrastructure health
bash scripts/test-infrastructure.sh

# Run end-to-end integration tests
bash scripts/test-e2e.sh

# Run load tests (100 jobs)
bash scripts/load-test.sh
```

### API Documentation

Once services are running, access interactive API documentation:

- **Orchestrator API**: http://localhost:8080/swagger-ui.html
- **Query Service API**: http://localhost:8085/swagger-ui.html

## Troubleshooting

### Services Won't Start

Check Docker resources:
```bash
docker-compose logs <service-name>
docker stats
```

### Kafka Connection Issues

Reset Kafka:
```bash
docker-compose down -v
docker-compose up -d
```

### Database Connection Failures

Verify database is ready:
```bash
docker exec -it orchestration-postgres pg_isready -U admin
```

For more troubleshooting help, see [VERIFICATION.md](VERIFICATION.md)

## Roadmap

- [x] **Phase 1**: Foundation & Infrastructure Setup
- [x] **Phase 2**: Orchestrator Service Implementation
- [x] **Phase 3**: Worker Service Implementation
- [x] **Phase 4**: Log Aggregation Service
- [x] **Phase 5**: Query Service & API
- [x] **Phase 6**: API Gateway & Rate Limiting
- [x] **Phase 7**: Monitoring Dashboards
- [x] **Phase 8**: Testing & Documentation
- [x] **Phase 9**: Security & Authentication - **COMPLETE!** 🔒

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Contact

For questions or support, please open an issue on GitHub.

---

**Built with Java 17, Spring Boot 3.2, and modern distributed systems technologies.**
