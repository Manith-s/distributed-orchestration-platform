package com.platform.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LogEntry represents a structured log message from any service in the platform.
 * <p>
 * These logs are:
 * - Sent to Kafka for real-time streaming
 * - Stored in ClickHouse for fast querying and analytics
 * - Indexed for full-text search
 * <p>
 * LogEntry supports correlation via jobId and tracking via workerId.
 * Additional context can be stored in the metadata map.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEntry {

    /**
     * Timestamp when the log was created
     * Uses ISO-8601 format in UTC timezone
     */
    @NotNull(message = "Timestamp cannot be null")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Associated job ID for correlation
     * Null if log is not job-specific (e.g., system logs)
     */
    private UUID jobId;

    /**
     * ID of the worker that generated this log
     * Format: "worker-{instance-id}" or service name for non-worker services
     */
    private String workerId;

    /**
     * Log level/severity
     * Standard levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL
     */
    @NotBlank(message = "Log level cannot be blank")
    @Builder.Default
    private String level = "INFO";

    /**
     * The actual log message
     * Should be concise and descriptive
     */
    @NotBlank(message = "Log message cannot be blank")
    private String message;

    /**
     * Additional contextual metadata as key-value pairs
     * Examples:
     * - {"userId": "123", "action": "login"}
     * - {"duration_ms": "450", "endpoint": "/api/jobs"}
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Name of the service that generated this log
     * Examples: "orchestrator-service", "worker-service", "log-aggregator-service"
     */
    @NotBlank(message = "Service name cannot be blank")
    private String serviceName;

    /**
     * Thread name that generated the log (optional)
     * Useful for debugging concurrency issues
     */
    private String threadName;

    /**
     * Exception stack trace if this is an error log (optional)
     * Stored as a string for easy searching
     */
    private String stackTrace;

    /**
     * Environment where the log was generated (optional)
     * Examples: "development", "staging", "production"
     */
    private String environment;

    /**
     * Adds a metadata entry
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Adds multiple metadata entries at once
     */
    public void addAllMetadata(Map<String, String> additionalMetadata) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.putAll(additionalMetadata);
    }

    /**
     * Checks if this is an error-level log
     */
    public boolean isError() {
        return "ERROR".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level);
    }

    /**
     * Checks if this is a warning-level log
     */
    public boolean isWarning() {
        return "WARN".equalsIgnoreCase(level);
    }

    /**
     * Creates a log entry for job-related logs
     */
    public static LogEntry forJob(UUID jobId, String level, String message, String serviceName) {
        return LogEntry.builder()
                .timestamp(Instant.now())
                .jobId(jobId)
                .level(level)
                .message(message)
                .serviceName(serviceName)
                .build();
    }

    /**
     * Creates a log entry for system/service logs (not job-specific)
     */
    public static LogEntry forService(String level, String message, String serviceName) {
        return LogEntry.builder()
                .timestamp(Instant.now())
                .level(level)
                .message(message)
                .serviceName(serviceName)
                .build();
    }

    /**
     * Creates an error log entry with stack trace
     */
    public static LogEntry forError(UUID jobId, String message, Throwable throwable, String serviceName) {
        return LogEntry.builder()
                .timestamp(Instant.now())
                .jobId(jobId)
                .level("ERROR")
                .message(message)
                .stackTrace(getStackTraceAsString(throwable))
                .serviceName(serviceName)
                .build();
    }

    /**
     * Converts a throwable to a string representation
     */
    private static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        if (throwable.getCause() != null) {
            sb.append("Caused by: ").append(getStackTraceAsString(throwable.getCause()));
        }
        return sb.toString();
    }
}
