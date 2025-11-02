# Deployment Guide

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- 8GB RAM minimum
- 20GB disk space

## Quick Start (Local Development)

### 1. Clone Repository
```bash
git clone <repository-url>
cd distributed-orchestration-platform
```

### 2. Start Infrastructure
```bash
docker-compose up -d postgres redis kafka clickhouse prometheus grafana
```

Wait ~30 seconds for services to initialize.

### 3. Build All Services
```bash
mvn clean install -DskipTests
```

### 4. Start Services

**Terminal 1 - Orchestrator:**
```bash
cd orchestrator-service
mvn spring-boot:run
```

**Terminal 2 - Worker (x3):**
```bash
cd worker-service
WORKER_ID=worker-1 SERVER_PORT=8081 mvn spring-boot:run
WORKER_ID=worker-2 SERVER_PORT=8082 mvn spring-boot:run
WORKER_ID=worker-3 SERVER_PORT=8083 mvn spring-boot:run
```

**Terminal 3 - Log Aggregator:**
```bash
cd log-aggregator-service
mvn spring-boot:run
```

**Terminal 4 - Query Service:**
```bash
cd query-service
mvn spring-boot:run
```

**Terminal 5 - API Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```

### 5. Verify Deployment
```bash
bash scripts/test-infrastructure.sh
bash scripts/test-e2e.sh
```

## Service Endpoints

| Service | Port | Health Check |
|---------|------|--------------|
| API Gateway | 8000 | http://localhost:8000/actuator/health |
| Orchestrator | 8080 | http://localhost:8080/actuator/health |
| Worker 1 | 8081 | http://localhost:8081/actuator/health |
| Worker 2 | 8082 | http://localhost:8082/actuator/health |
| Worker 3 | 8083 | http://localhost:8083/actuator/health |
| Log Aggregator | 8084 | http://localhost:8084/health |
| Query Service | 8085 | http://localhost:8085/actuator/health |
| Grafana | 3000 | http://localhost:3000 |
| Prometheus | 9090 | http://localhost:9090 |

## API Documentation

- **Orchestrator API**: http://localhost:8080/swagger-ui.html
- **Query Service API**: http://localhost:8085/swagger-ui.html

## Production Deployment

### Docker Compose (Production)

Build all services:
```bash
mvn clean package -DskipTests
```

Create `docker-compose.prod.yml` with all services.

Start production stack:
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes Deployment

See `k8s/` directory for Kubernetes manifests.

### Configuration

Production settings in `application-prod.yml` files:
- Increase connection pools
- Enable SSL/TLS
- Configure authentication
- Set proper resource limits

## Monitoring

Access Grafana: http://localhost:3000
- Username: admin
- Password: admin

Dashboards:
1. Job Orchestration Dashboard
2. System Health Dashboard
3. Log Analytics Dashboard

## Troubleshooting

### Services won't start
- Check Docker containers: `docker-compose ps`
- Check logs: `docker-compose logs [service]`
- Verify ports not in use: `netstat -an | grep LISTEN`

### Jobs not processing
- Check worker logs
- Verify Kafka connectivity
- Check Redis locks: `docker exec -it orchestration-redis redis-cli`

### High latency
- Scale workers: Start more worker instances
- Increase Kafka partitions
- Optimize database queries

## Scaling

### Horizontal Scaling
- **Workers**: Start additional worker instances with unique WORKER_ID
- **Orchestrator**: Run multiple instances behind load balancer
- **Query Service**: Run multiple read replicas

### Vertical Scaling
- Increase JVM heap: `-Xmx4g -Xms2g`
- Increase database connections
- Increase Kafka consumers

## Backup & Recovery

### Database Backup
```bash
docker exec orchestration-postgres pg_dump -U postgres jobs_db > backup.sql
```

### ClickHouse Backup
```bash
docker exec orchestration-clickhouse clickhouse-client --query "BACKUP DATABASE logs_db TO '/backups/logs_db'"
```

## Security

### 1. Create Environment Variables

Copy `.env.example` to `.env` and update with secure passwords:

```bash
cp .env.example .env
# Edit .env with your secure passwords
```

**IMPORTANT:** Never commit `.env` file to version control!

### 2. Generate Secure Passwords

Use strong passwords for production:

```bash
# Generate secure password (Linux/Mac)
openssl rand -base64 32

# Or use:
pwgen 32 1

# For Windows (PowerShell)
[System.Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

### 3. Authentication

The platform uses JWT authentication. To obtain a token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "expiresIn": 86400000
}
```

### 4. Using the JWT Token

Include the token in all API requests:

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Job",
    "type": "EMAIL",
    "payload": "{}",
    "priority": 1
  }'
```

### 5. Default Users

**Development only - Change in production!**

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | ADMIN |
| user     | user123   | USER  |

### 6. User Registration

Register a new user account:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "SecurePassword123!",
    "email": "newuser@example.com",
    "roles": ["ROLE_USER"]
  }'
```

### 7. Query Service Authentication

The query service uses HTTP Basic authentication:

```bash
curl -u admin:admin123 http://localhost:8085/api/v1/logs/search \
  -H "Content-Type: application/json" \
  -d '{
    "jobId": "job-123",
    "startTime": "2024-01-01T00:00:00",
    "endTime": "2024-12-31T23:59:59"
  }'
```

### 8. Production Security Checklist

- [ ] Change all default passwords
- [ ] Use strong, unique passwords (32+ characters)
- [ ] Generate secure JWT secret (minimum 256 bits)
- [ ] Enable HTTPS/TLS for all services
- [ ] Rotate JWT secret regularly
- [ ] Set up firewall rules
- [ ] Configure proper database user permissions
- [ ] Enable audit logging
- [ ] Regular security updates and dependency scanning
- [ ] Use secrets management (Vault, AWS Secrets Manager)
- [ ] Enable rate limiting on API Gateway
- [ ] Configure CORS policies appropriately
- [ ] Implement password complexity requirements
- [ ] Set up monitoring and alerting for security events
- [ ] Use network isolation for sensitive services
- [ ] Disable unnecessary actuator endpoints in production

