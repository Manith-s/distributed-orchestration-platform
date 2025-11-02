-- =====================================================
-- PostgreSQL Initialization Script
-- Database: orchestration_db
-- Purpose: Job state management and metadata storage
-- =====================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- =====================================================
-- JOBS TABLE
-- Stores all job information with ACID guarantees
-- =====================================================

CREATE TABLE IF NOT EXISTS jobs (
    -- Primary identifier
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Job metadata
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,

    -- Job data (stored as JSONB for flexibility and indexing)
    payload JSONB,

    -- Priority and retry configuration
    priority INTEGER DEFAULT 0 NOT NULL,
    retry_count INTEGER DEFAULT 0 NOT NULL,
    max_retries INTEGER DEFAULT 3 NOT NULL,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    -- Error tracking
    error_message TEXT,

    -- Worker assignment
    worker_id VARCHAR(100),

    -- Constraints
    CONSTRAINT valid_priority CHECK (priority >= 0),
    CONSTRAINT valid_retry_count CHECK (retry_count >= 0),
    CONSTRAINT valid_max_retries CHECK (max_retries >= 0),
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'RETRYING', 'DEAD_LETTER'))
);

-- =====================================================
-- INDEXES for Query Performance
-- =====================================================

-- Index for status-based queries (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_jobs_status
ON jobs(status)
WHERE status IN ('PENDING', 'QUEUED', 'RUNNING', 'RETRYING');

-- Index for time-based queries (job history, analytics)
CREATE INDEX IF NOT EXISTS idx_jobs_created_at
ON jobs(created_at DESC);

-- Index for job type filtering
CREATE INDEX IF NOT EXISTS idx_jobs_type
ON jobs(type);

-- Composite index for priority-based job scheduling
CREATE INDEX IF NOT EXISTS idx_jobs_priority_created
ON jobs(priority DESC, created_at ASC)
WHERE status IN ('PENDING', 'QUEUED');

-- Index for worker assignment tracking
CREATE INDEX IF NOT EXISTS idx_jobs_worker_id
ON jobs(worker_id)
WHERE worker_id IS NOT NULL;

-- Index for failed jobs requiring retry
CREATE INDEX IF NOT EXISTS idx_jobs_failed_retry
ON jobs(status, retry_count, max_retries)
WHERE status = 'FAILED';

-- GIN index for JSONB payload queries (enables fast JSON searches)
CREATE INDEX IF NOT EXISTS idx_jobs_payload_gin
ON jobs USING GIN (payload jsonb_path_ops);

-- =====================================================
-- PERFORMANCE VIEWS
-- =====================================================

-- View for job statistics by status
CREATE OR REPLACE VIEW job_status_stats AS
SELECT
    status,
    COUNT(*) as count,
    AVG(EXTRACT(EPOCH FROM (completed_at - started_at))) as avg_duration_seconds,
    MIN(created_at) as oldest_job,
    MAX(created_at) as newest_job
FROM jobs
GROUP BY status;

-- View for job statistics by type
CREATE OR REPLACE VIEW job_type_stats AS
SELECT
    type,
    COUNT(*) as total_jobs,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_jobs,
    COUNT(*) FILTER (WHERE status = 'FAILED' OR status = 'DEAD_LETTER') as failed_jobs,
    AVG(retry_count) as avg_retries,
    AVG(EXTRACT(EPOCH FROM (completed_at - started_at))) FILTER (WHERE status = 'COMPLETED') as avg_duration_seconds
FROM jobs
GROUP BY type;

-- View for worker performance metrics
CREATE OR REPLACE VIEW worker_performance_stats AS
SELECT
    worker_id,
    COUNT(*) as jobs_processed,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as successful_jobs,
    COUNT(*) FILTER (WHERE status = 'FAILED' OR status = 'DEAD_LETTER') as failed_jobs,
    AVG(EXTRACT(EPOCH FROM (completed_at - started_at))) as avg_processing_time_seconds
FROM jobs
WHERE worker_id IS NOT NULL
GROUP BY worker_id;

-- =====================================================
-- FUNCTIONS for Job Management
-- =====================================================

-- Function to get next pending job by priority
CREATE OR REPLACE FUNCTION get_next_pending_job()
RETURNS TABLE (
    job_id UUID,
    job_name VARCHAR,
    job_type VARCHAR,
    job_payload JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT id, name, type, payload
    FROM jobs
    WHERE status = 'PENDING'
    ORDER BY priority DESC, created_at ASC
    LIMIT 1
    FOR UPDATE SKIP LOCKED;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old completed jobs (for data retention)
CREATE OR REPLACE FUNCTION cleanup_old_jobs(retention_days INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM jobs
    WHERE status IN ('COMPLETED', 'DEAD_LETTER')
    AND completed_at < CURRENT_TIMESTAMP - (retention_days || ' days')::INTERVAL;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- TRIGGERS for Audit and Validation
-- =====================================================

-- Trigger to validate job state transitions
CREATE OR REPLACE FUNCTION validate_job_status_transition()
RETURNS TRIGGER AS $$
BEGIN
    -- Prevent modification of completed or dead-letter jobs
    IF OLD.status IN ('COMPLETED', 'DEAD_LETTER') AND NEW.status != OLD.status THEN
        RAISE EXCEPTION 'Cannot modify job in terminal state: %', OLD.status;
    END IF;

    -- Auto-set timestamps based on status
    IF NEW.status = 'RUNNING' AND OLD.status != 'RUNNING' THEN
        NEW.started_at = CURRENT_TIMESTAMP;
    END IF;

    IF NEW.status IN ('COMPLETED', 'FAILED', 'DEAD_LETTER') AND OLD.status NOT IN ('COMPLETED', 'FAILED', 'DEAD_LETTER') THEN
        NEW.completed_at = CURRENT_TIMESTAMP;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_job_status_transition
BEFORE UPDATE ON jobs
FOR EACH ROW
EXECUTE FUNCTION validate_job_status_transition();

-- =====================================================
-- SAMPLE DATA (for development/testing)
-- =====================================================

-- Uncomment to insert sample jobs
-- INSERT INTO jobs (name, type, status, payload, priority) VALUES
-- ('Send Welcome Email', 'EMAIL', 'PENDING', '{"to": "user@example.com", "template": "welcome"}', 5),
-- ('Generate Monthly Report', 'REPORT', 'PENDING', '{"month": "2024-04", "format": "PDF"}', 8),
-- ('Sync User Data', 'DATA_SYNC', 'QUEUED', '{"userId": "12345", "source": "CRM"}', 3);

-- =====================================================
-- GRANTS (if using specific application user)
-- =====================================================

-- Uncomment and modify if you need a specific application user
-- CREATE USER orchestrator_app WITH PASSWORD 'secure_password';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON jobs TO orchestrator_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO orchestrator_app;

-- =====================================================
-- USERS TABLE (for authentication)
-- Stores user credentials and roles
-- =====================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    enabled BOOLEAN DEFAULT TRUE NOT NULL,
    account_non_expired BOOLEAN DEFAULT TRUE NOT NULL,
    account_non_locked BOOLEAN DEFAULT TRUE NOT NULL,
    credentials_non_expired BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Create indexes for user lookup
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Insert default admin user (password: admin123)
-- Password is BCrypt hash of "admin123"
INSERT INTO users (username, password, email, enabled, account_non_expired, account_non_locked, credentials_non_expired)
VALUES ('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'admin@platform.com', true, true, true, true)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER' FROM users WHERE username = 'admin'
ON CONFLICT DO NOTHING;

-- =====================================================
-- VACUUM and ANALYZE for optimal performance
-- =====================================================

VACUUM ANALYZE jobs;
VACUUM ANALYZE users;

-- =====================================================
-- Completion Message
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'PostgreSQL initialization completed!';
    RAISE NOTICE 'Database: orchestration_db';
    RAISE NOTICE 'Tables created: jobs, users, user_roles';
    RAISE NOTICE 'Views created: 3';
    RAISE NOTICE 'Functions created: 2';
    RAISE NOTICE 'Triggers created: 1';
    RAISE NOTICE 'Default admin user created: admin/admin123';
    RAISE NOTICE '============================================';
END $$;
