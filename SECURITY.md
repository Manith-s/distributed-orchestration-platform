# Security Guide

## Overview

The Distributed Orchestration Platform implements JWT-based authentication and follows security best practices for production deployment.

---

## Authentication

### JWT (JSON Web Token) Authentication

The platform uses stateless JWT authentication for API access.

---

## Getting Started

### 1. Obtain JWT Token

**Login Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "admin"
}
```

### 2. Use JWT Token

Include the token in the `Authorization` header for all protected endpoints:

```bash
curl -X GET http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

**Example with job submission:**
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

### 3. Register New User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "SecurePass123!",
    "email": "newuser@example.com",
    "roles": ["ROLE_USER"]
  }'
```

---

## Default Users

**Development only - Change in production!**

### Orchestrator Service (JWT Auth)

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | ADMIN |
| user     | user123   | USER  |

### Query Service (Basic Auth)

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | ADMIN |
| user     | user123   | USER  |

**⚠️ CRITICAL**: Change all default passwords immediately in production!

---

## Security Configuration

### Environment Variables

All sensitive configuration should be set via environment variables:

#### Required Secrets

```bash
# JWT Configuration
export JWT_SECRET="CHANGE_TO_LONG_RANDOM_STRING_MIN_256_BITS"
export JWT_EXPIRATION=86400000  # 24 hours in milliseconds

# Database
export POSTGRES_PASSWORD="YOUR_SECURE_DB_PASSWORD"

# Redis
export REDIS_PASSWORD="YOUR_SECURE_REDIS_PASSWORD"

# ClickHouse
export CLICKHOUSE_PASSWORD="YOUR_SECURE_CLICKHOUSE_PASSWORD"

# Grafana
export GRAFANA_ADMIN_PASSWORD="YOUR_SECURE_GRAFANA_PASSWORD"
```

#### Generate Secure JWT Secret

```bash
# Linux/Mac
openssl rand -base64 64

# Or use online tool (in secure environment)
# https://www.grc.com/passwords.htm
```

---

## Public vs Protected Endpoints

### Public Endpoints (No Authentication Required)

- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/register` - User registration
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Metrics (should be restricted in production)
- `GET /swagger-ui/**` - API documentation (should be restricted in production)
- `GET /v3/api-docs/**` - OpenAPI spec

### Protected Endpoints (JWT Required)

- `POST /api/v1/jobs` - Submit new job
- `GET /api/v1/jobs` - List jobs
- `GET /api/v1/jobs/{id}` - Get job details
- `GET /api/v1/jobs/stats` - Get job statistics
- All other API endpoints

---

## Role-Based Access Control (RBAC)

### Available Roles

- `ROLE_USER` - Standard user (read/write jobs)
- `ROLE_ADMIN` - Administrator (full access)

### Adding Custom Roles

1. Add new role to user during registration:
```json
{
  "username": "manager",
  "password": "SecurePass123!",
  "email": "manager@example.com",
  "roles": ["ROLE_USER", "ROLE_MANAGER"]
}
```

2. Use `@PreAuthorize` annotation in controllers:
```java
@PreAuthorize("hasRole('ROLE_ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteJob(@PathVariable UUID id) {
    // Only admins can delete jobs
}
```

---

## Production Security Checklist

### Before Deployment

- [ ] Change default admin password
- [ ] Generate strong JWT secret (min 256 bits)
- [ ] Set all passwords via environment variables
- [ ] Enable HTTPS/TLS (see TLS Configuration below)
- [ ] Configure firewall rules
- [ ] Disable Swagger UI in production (or restrict access)
- [ ] Configure CORS properly
- [ ] Set up rate limiting on API Gateway
- [ ] Enable audit logging
- [ ] Configure session timeout
- [ ] Set up intrusion detection
- [ ] Perform security audit/penetration testing

### After Deployment

- [ ] Monitor authentication logs
- [ ] Rotate JWT secrets regularly (every 90 days)
- [ ] Monitor failed login attempts
- [ ] Keep dependencies updated
- [ ] Regular security patches
- [ ] Backup encryption keys

---

## TLS/HTTPS Configuration

### Option 1: Using Reverse Proxy (Recommended)

Use Nginx or Apache as reverse proxy with Let's Encrypt:

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Option 2: Spring Boot Native TLS

Add to `application.yml`:

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: platform
```

Generate keystore:
```bash
keytool -genkeypair -alias platform -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650
```

---

## Password Policy

### Recommendations

- Minimum 8 characters
- Must contain uppercase, lowercase, number, and special character
- No dictionary words
- Password expiration: 90 days
- Password history: last 5 passwords
- Account lockout after 5 failed attempts

### Implementation

Update `RegisterRequest` validation:

```java
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
    message = "Password must be at least 8 characters and contain uppercase, lowercase, number, and special character"
)
private String password;
```

---

## Common Vulnerabilities & Mitigations

### SQL Injection
✅ **Protected** - Using JPA with parameterized queries

### XSS (Cross-Site Scripting)
✅ **Protected** - Input validation with `@Valid` annotations

### CSRF (Cross-Site Request Forgery)
✅ **Protected** - Stateless JWT (no session cookies)

### Brute Force Attacks
⚠️ **Recommendation**: Implement rate limiting on `/auth/login`

### Session Fixation
✅ **Protected** - Stateless authentication (no sessions)

### Sensitive Data Exposure
✅ **Protected** - Passwords hashed with BCrypt
⚠️ **Action Required**: Enable HTTPS

---

## Monitoring & Auditing

### Authentication Events to Monitor

1. **Failed Login Attempts**
   - Alert after 5 failed attempts
   - Lock account after 10 failed attempts

2. **Successful Logins**
   - Log IP address, timestamp, user agent
   - Alert on login from new location

3. **Token Expiration**
   - Monitor expired token usage attempts

4. **Permission Denied Events**
   - Log unauthorized access attempts

### Implementation

Add to `AuthenticationController`:

```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody AuthenticationRequest request,
                                HttpServletRequest httpRequest) {
    String ipAddress = httpRequest.getRemoteAddr();
    log.info("Login attempt from IP: {} for user: {}", ipAddress, request.getUsername());
    // ... authentication logic
}
```

---

## Incident Response

### Suspected Compromise

1. **Immediate Actions**:
   - Rotate JWT secret
   - Invalidate all existing tokens
   - Review recent authentication logs
   - Check for unauthorized access

2. **Investigation**:
   - Analyze access logs
   - Identify compromised accounts
   - Determine scope of breach

3. **Recovery**:
   - Force password reset for affected users
   - Patch vulnerabilities
   - Update security measures

---

## API Security Best Practices

### Input Validation

```java
@PostMapping
public ResponseEntity<?> submitJob(@Valid @RequestBody JobRequest request) {
    // Validation happens automatically
}
```

### Output Encoding

```java
// Never expose sensitive data in responses
@JsonIgnore
private String password;
```

### Rate Limiting

Configured in API Gateway:

```yaml
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 100
    redis-rate-limiter.burstCapacity: 200
```

---

## Security Headers

Add to Spring Security configuration:

```java
http.headers()
    .contentSecurityPolicy("default-src 'self'")
    .and()
    .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
    .and()
    .permissionsPolicy(policy ->
        policy.policy("geolocation=(), camera=(), microphone=()"))
    .and()
    .xssProtection()
    .and()
    .frameOptions().deny();
```

---

## Contact

For security issues, please contact: security@platform.com

**Do not disclose security vulnerabilities publicly.**

---

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
