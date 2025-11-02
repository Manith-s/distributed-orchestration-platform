package com.platform.common.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Job entity representing a distributed task to be executed by workers.
 * <p>
 * Jobs progress through various states: PENDING -> QUEUED -> RUNNING -> COMPLETED/FAILED
 * Failed jobs can be retried up to maxRetries times before being moved to DEAD_LETTER.
 * <p>
 * The payload field stores job-specific data as JSON for flexibility.
 * Priority determines execution order (higher values = higher priority).
 */
@Entity
@Table(
    name = "jobs",
    indexes = {
        @Index(name = "idx_jobs_status", columnList = "status"),
        @Index(name = "idx_jobs_created_at", columnList = "created_at"),
        @Index(name = "idx_jobs_type", columnList = "type"),
        @Index(name = "idx_jobs_priority", columnList = "priority")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "payload") // Exclude large payload from toString
public class Job {

    /**
     * Unique identifier for the job (UUID v4)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Human-readable name for the job (e.g., "Send Welcome Email", "Sync User Data")
     */
    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Job name cannot be blank")
    private String name;

    /**
     * Job type/category for routing and processing
     * Examples: EMAIL, DATA_SYNC, REPORT_GENERATION, IMAGE_PROCESSING
     */
    @Column(name = "type", nullable = false, length = 100)
    @NotBlank(message = "Job type cannot be blank")
    private String type;

    /**
     * Current status of the job in its lifecycle
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @NotNull(message = "Job status cannot be null")
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    /**
     * Job-specific data stored as JSON
     * This allows flexible payload structures for different job types
     * Example: {"email": "user@example.com", "template": "welcome"}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    /**
     * Job priority (higher = more important)
     * Range: 0-10, where 10 is highest priority
     * Default: 0 (normal priority)
     */
    @Column(name = "priority", nullable = false)
    @Min(value = 0, message = "Priority cannot be negative")
    @Builder.Default
    private Integer priority = 0;

    /**
     * Number of times this job has been retried
     * Incremented each time a failed job is requeued
     */
    @Column(name = "retry_count", nullable = false)
    @Min(value = 0, message = "Retry count cannot be negative")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maximum number of retry attempts before moving to DEAD_LETTER
     * Default: 3 retries
     */
    @Column(name = "max_retries", nullable = false)
    @Min(value = 0, message = "Max retries cannot be negative")
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Timestamp when the job was created
     * Automatically set by Hibernate
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when a worker started processing this job
     * Null if job hasn't started yet
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Timestamp when the job finished (either COMPLETED or FAILED)
     * Null if job is still in progress
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Error message if job failed
     * Stores the last error message (up to 2000 characters)
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /**
     * ID of the worker that is/was processing this job
     * Format: "worker-{instance-id}" (e.g., "worker-1", "worker-2")
     */
    @Column(name = "worker_id", length = 100)
    private String workerId;

    /**
     * Calculates the duration of job execution in seconds
     * Returns null if job hasn't started or hasn't completed
     */
    public Long getDurationInSeconds() {
        if (startedAt != null && completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
        return null;
    }

    /**
     * Checks if the job can be retried
     * A job can be retried if it hasn't exceeded max retries
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Increments the retry count
     * Should be called when requeuing a failed job
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Marks the job as started by a worker
     */
    public void markAsStarted(String workerId) {
        this.workerId = workerId;
        this.startedAt = LocalDateTime.now();
        this.status = JobStatus.RUNNING;
    }

    /**
     * Marks the job as completed successfully
     */
    public void markAsCompleted() {
        this.completedAt = LocalDateTime.now();
        this.status = JobStatus.COMPLETED;
        this.errorMessage = null;
    }

    /**
     * Marks the job as failed with an error message
     */
    public void markAsFailed(String errorMessage) {
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage != null && errorMessage.length() > 2000
            ? errorMessage.substring(0, 2000)
            : errorMessage;

        if (canRetry()) {
            this.status = JobStatus.FAILED;
        } else {
            this.status = JobStatus.DEAD_LETTER;
        }
    }
}
