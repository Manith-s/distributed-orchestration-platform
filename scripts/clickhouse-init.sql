-- =====================================================
-- ClickHouse Initialization Script
-- Database: logs_db
-- Purpose: High-performance log aggregation and analytics
-- =====================================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS logs_db;

USE logs_db;

-- =====================================================
-- JOB LOGS TABLE
-- Stores all log entries from services with time-series optimization
-- =====================================================

CREATE TABLE IF NOT EXISTS job_logs (
    -- Timestamp with millisecond precision
    timestamp DateTime64(3) DEFAULT now64(3),

    -- Job correlation
    job_id UUID,

    -- Service and worker identification
    worker_id String,
    service_name LowCardinality(String),  -- LowCardinality for repeated values

    -- Log metadata
    level LowCardinality(String),  -- INFO, WARN, ERROR, etc.
    message String,

    -- Additional context
    metadata String,  -- JSON string for flexible key-value pairs
    thread_name String,
    stack_trace String,
    environment LowCardinality(String),  -- dev, staging, prod

    -- Ingestion metadata
    ingested_at DateTime DEFAULT now()
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)  -- Monthly partitions for efficient data management
ORDER BY (timestamp, service_name, level, job_id)
SETTINGS
    index_granularity = 8192,
    ttl_only_drop_parts = 1;

-- TTL: Automatically drop partitions older than 90 days
ALTER TABLE job_logs
MODIFY TTL timestamp + INTERVAL 90 DAY;

-- =====================================================
-- JOB METRICS TABLE
-- Stores quantitative metrics for jobs and system performance
-- =====================================================

CREATE TABLE IF NOT EXISTS job_metrics (
    -- Timestamp
    timestamp DateTime64(3) DEFAULT now64(3),

    -- Metric identification
    metric_name LowCardinality(String),  -- e.g., "job.duration", "job.retry_count"
    metric_value Float64,

    -- Correlation
    job_id UUID,
    job_type LowCardinality(String),

    -- Dimensions for filtering
    tags String,  -- JSON string for flexible tagging
    service_name LowCardinality(String),
    worker_id String,

    -- Ingestion metadata
    ingested_at DateTime DEFAULT now()
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (timestamp, metric_name, job_type)
SETTINGS
    index_granularity = 8192,
    ttl_only_drop_parts = 1;

-- TTL: Automatically drop partitions older than 180 days (6 months)
ALTER TABLE job_metrics
MODIFY TTL timestamp + INTERVAL 180 DAY;

-- =====================================================
-- MATERIALIZED VIEWS for Real-time Aggregations
-- =====================================================

-- Hourly log count by service and level
CREATE TABLE IF NOT EXISTS logs_hourly_stats (
    hour DateTime,
    service_name LowCardinality(String),
    level LowCardinality(String),
    log_count UInt64,
    error_count UInt64,
    warning_count UInt64
)
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(hour)
ORDER BY (hour, service_name, level)
SETTINGS index_granularity = 8192;

CREATE MATERIALIZED VIEW IF NOT EXISTS logs_hourly_stats_mv
TO logs_hourly_stats
AS SELECT
    toStartOfHour(timestamp) as hour,
    service_name,
    level,
    count() as log_count,
    countIf(level = 'ERROR') as error_count,
    countIf(level = 'WARN') as warning_count
FROM job_logs
GROUP BY hour, service_name, level;

-- Hourly metrics aggregation
CREATE TABLE IF NOT EXISTS metrics_hourly_agg (
    hour DateTime,
    metric_name LowCardinality(String),
    job_type LowCardinality(String),
    avg_value AggregateFunction(avg, Float64),
    max_value AggregateFunction(max, Float64),
    min_value AggregateFunction(min, Float64),
    count_value AggregateFunction(count, Float64)
)
ENGINE = AggregatingMergeTree()
PARTITION BY toYYYYMM(hour)
ORDER BY (hour, metric_name, job_type)
SETTINGS index_granularity = 8192;

CREATE MATERIALIZED VIEW IF NOT EXISTS metrics_hourly_agg_mv
TO metrics_hourly_agg
AS SELECT
    toStartOfHour(timestamp) as hour,
    metric_name,
    job_type,
    avgState(metric_value) as avg_value,
    maxState(metric_value) as max_value,
    minState(metric_value) as min_value,
    countState(metric_value) as count_value
FROM job_metrics
GROUP BY hour, metric_name, job_type;

-- Daily job completion stats
CREATE TABLE IF NOT EXISTS job_daily_stats (
    date Date,
    job_type LowCardinality(String),
    total_jobs UInt64,
    completed_jobs UInt64,
    failed_jobs UInt64,
    avg_duration Float64
)
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (date, job_type)
SETTINGS index_granularity = 8192;

-- =====================================================
-- QUERY OPTIMIZATION VIEWS
-- =====================================================

-- Recent error logs (last 24 hours)
CREATE VIEW IF NOT EXISTS recent_errors AS
SELECT
    timestamp,
    service_name,
    worker_id,
    job_id,
    message,
    stack_trace
FROM job_logs
WHERE
    level IN ('ERROR', 'FATAL')
    AND timestamp >= now() - INTERVAL 24 HOUR
ORDER BY timestamp DESC;

-- Job performance summary
CREATE VIEW IF NOT EXISTS job_performance_summary AS
SELECT
    job_type,
    count() as total_executions,
    avg(metric_value) as avg_duration_ms,
    quantile(0.5)(metric_value) as p50_duration_ms,
    quantile(0.95)(metric_value) as p95_duration_ms,
    quantile(0.99)(metric_value) as p99_duration_ms,
    max(metric_value) as max_duration_ms
FROM job_metrics
WHERE metric_name = 'job.duration'
GROUP BY job_type;

-- Error rate by service (last 1 hour)
CREATE VIEW IF NOT EXISTS error_rate_by_service AS
SELECT
    service_name,
    countIf(level = 'ERROR') as error_count,
    countIf(level = 'WARN') as warning_count,
    count() as total_logs,
    (error_count / total_logs) * 100 as error_rate_percent
FROM job_logs
WHERE timestamp >= now() - INTERVAL 1 HOUR
GROUP BY service_name
ORDER BY error_rate_percent DESC;

-- =====================================================
-- CUSTOM FUNCTIONS for Analytics
-- =====================================================

-- Function to get hourly metrics with proper aggregation merging
CREATE VIEW IF NOT EXISTS hourly_metrics_summary AS
SELECT
    hour,
    metric_name,
    job_type,
    avgMerge(avg_value) as avg,
    maxMerge(max_value) as max,
    minMerge(min_value) as min,
    countMerge(count_value) as count
FROM metrics_hourly_agg
GROUP BY hour, metric_name, job_type
ORDER BY hour DESC;

-- =====================================================
-- INDEXES for Fast Queries
-- =====================================================

-- Skip index for faster job_id lookups
ALTER TABLE job_logs
ADD INDEX idx_job_id job_id TYPE bloom_filter GRANULARITY 1;

-- Skip index for service_name filtering
ALTER TABLE job_logs
ADD INDEX idx_service service_name TYPE set(100) GRANULARITY 1;

-- Skip index for log level filtering
ALTER TABLE job_logs
ADD INDEX idx_level level TYPE set(10) GRANULARITY 1;

-- Skip index for metric_name in metrics table
ALTER TABLE job_metrics
ADD INDEX idx_metric_name metric_name TYPE set(100) GRANULARITY 1;

-- =====================================================
-- SAMPLE QUERIES for Testing
-- =====================================================

-- These are example queries - uncomment to test after data ingestion

-- Query 1: Get all ERROR logs from the last hour
-- SELECT * FROM job_logs
-- WHERE level = 'ERROR' AND timestamp >= now() - INTERVAL 1 HOUR
-- ORDER BY timestamp DESC
-- LIMIT 100;

-- Query 2: Get job duration statistics by type
-- SELECT
--     job_type,
--     count() as executions,
--     avg(metric_value) as avg_duration_ms,
--     quantile(0.95)(metric_value) as p95_ms
-- FROM job_metrics
-- WHERE metric_name = 'job.duration'
-- GROUP BY job_type;

-- Query 3: Get error count by service in the last 24 hours
-- SELECT
--     service_name,
--     count() as error_count
-- FROM job_logs
-- WHERE level = 'ERROR' AND timestamp >= now() - INTERVAL 24 HOUR
-- GROUP BY service_name
-- ORDER BY error_count DESC;

-- Query 4: Search logs by message content
-- SELECT * FROM job_logs
-- WHERE message LIKE '%failed%'
-- AND timestamp >= now() - INTERVAL 6 HOUR
-- ORDER BY timestamp DESC
-- LIMIT 50;

-- =====================================================
-- DATA RETENTION POLICIES
-- =====================================================

-- Automatically optimize tables to remove expired partitions
OPTIMIZE TABLE job_logs FINAL;
OPTIMIZE TABLE job_metrics FINAL;

-- =====================================================
-- Performance Settings
-- =====================================================

-- Set system settings for better performance
-- These would typically be set in config.xml, but shown here for reference

-- SYSTEM SET max_memory_usage = 10000000000;  -- 10GB
-- SYSTEM SET max_execution_time = 300;  -- 5 minutes
-- SYSTEM SET max_threads = 8;

-- =====================================================
-- Table Statistics
-- =====================================================

SELECT
    'Initialization Complete!' as status,
    'Tables Created: job_logs, job_metrics' as tables,
    'Materialized Views: 3' as views,
    'Indexes: 4 skip indexes' as indexes,
    'TTL Policies: 90 days (logs), 180 days (metrics)' as retention;
