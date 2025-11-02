package com.platform.common.model;

/**
 * Enum representing the lifecycle states of a Job in the orchestration platform.
 * <p>
 * State Transitions:
 * PENDING -> QUEUED -> RUNNING -> COMPLETED (success path)
 *                             -> FAILED -> RETRYING -> RUNNING (retry path)
 *                             -> DEAD_LETTER (permanent failure after max retries)
 * <p>
 * State Descriptions:
 * - PENDING: Job created but not yet sent to message queue
 * - QUEUED: Job sent to Kafka and waiting for a worker to pick it up
 * - RUNNING: Job being actively processed by a worker
 * - COMPLETED: Job finished successfully
 * - FAILED: Job failed but has retries remaining
 * - RETRYING: Job is being retried after a failure
 * - DEAD_LETTER: Job permanently failed after exhausting all retries
 */
public enum JobStatus {

    /**
     * Initial state - job has been created but not yet queued
     * Transitions to: QUEUED
     */
    PENDING("Job is pending and not yet queued"),

    /**
     * Job has been sent to Kafka and is waiting for a worker
     * Transitions to: RUNNING
     */
    QUEUED("Job is queued and waiting for worker assignment"),

    /**
     * Job is currently being processed by a worker
     * Transitions to: COMPLETED, FAILED
     */
    RUNNING("Job is actively being processed by a worker"),

    /**
     * Job completed successfully
     * Terminal state (no further transitions)
     */
    COMPLETED("Job completed successfully"),

    /**
     * Job failed during execution but can be retried
     * Transitions to: RETRYING, DEAD_LETTER (if max retries exceeded)
     */
    FAILED("Job failed but can be retried"),

    /**
     * Job is being retried after a previous failure
     * Transitions to: QUEUED, RUNNING
     */
    RETRYING("Job is being retried after failure"),

    /**
     * Job permanently failed after exhausting all retry attempts
     * Terminal state (requires manual intervention)
     */
    DEAD_LETTER("Job permanently failed after max retries");

    private final String description;

    JobStatus(String description) {
        this.description = description;
    }

    /**
     * Gets the human-readable description of this status
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status represents a terminal state
     * Terminal states: COMPLETED, DEAD_LETTER
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == DEAD_LETTER;
    }

    /**
     * Checks if this status represents a failure state
     * Failure states: FAILED, DEAD_LETTER
     */
    public boolean isFailure() {
        return this == FAILED || this == DEAD_LETTER;
    }

    /**
     * Checks if this status represents an active state (job is in progress)
     * Active states: QUEUED, RUNNING, RETRYING
     */
    public boolean isActive() {
        return this == QUEUED || this == RUNNING || this == RETRYING;
    }

    /**
     * Checks if job in this status can be cancelled
     * Cancellable states: PENDING, QUEUED
     */
    public boolean isCancellable() {
        return this == PENDING || this == QUEUED;
    }

    @Override
    public String toString() {
        return name() + ": " + description;
    }
}
