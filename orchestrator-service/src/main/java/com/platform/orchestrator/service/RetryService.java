package com.platform.orchestrator.service;

import com.platform.common.model.Job;
import com.platform.common.model.JobStatus;
import com.platform.orchestrator.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetryService {

    private final JobRepository jobRepository;
    private final JobDistributionService distributionService;
    private final MetricsService metricsService;

    @Value("${orchestrator.retry.initial-backoff}")
    private long initialBackoff;

    @Value("${orchestrator.retry.max-backoff}")
    private long maxBackoff;

    /**
     * Poll for failed jobs eligible for retry.
     * Runs every 30 seconds (configured in application.yml).
     */
    @Scheduled(fixedDelayString = "${orchestrator.retry.interval}")
    @Transactional
    public void processRetries() {
        try {
            // Find failed jobs eligible for retry
            List<Job> jobsForRetry = jobRepository.findJobsForRetry(
                LocalDateTime.now()
            );

            if (jobsForRetry.isEmpty()) {
                log.debug("No jobs eligible for retry");
                return;
            }

            log.info("Retrying {} failed jobs", jobsForRetry.size());

            for (Job job : jobsForRetry) {
                try {
                    retryJob(job);
                } catch (Exception e) {
                    log.error("Failed to retry job: id={}", job.getId(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error in retry service", e);
        }
    }

    /**
     * Retry a failed job with exponential backoff.
     */
    @Transactional
    public void retryJob(Job job) {
        // Calculate backoff time
        long backoffMs = calculateBackoff(job.getRetryCount());
        LocalDateTime retryTime = job.getCompletedAt()
            .plusNanos(backoffMs * 1_000_000);

        // Check if enough time has passed
        if (LocalDateTime.now().isBefore(retryTime)) {
            log.debug("Job not yet eligible for retry: jobId={}, retryTime={}",
                    job.getId(), retryTime);
            return;
        }

        log.info("Retrying job: id={}, attempt={}/{}",
            job.getId(), job.getRetryCount() + 1, job.getMaxRetries());

        // Update job for retry
        job.setStatus(JobStatus.RETRYING);
        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(null);
        jobRepository.save(job);

        // Record metrics
        metricsService.recordJobRetry(job.getType());

        // Distribute the job
        distributionService.distributeJob(job);
    }

    /**
     * Calculate exponential backoff time in milliseconds.
     */
    private long calculateBackoff(int retryCount) {
        long backoff = initialBackoff * (long) Math.pow(2, retryCount);
        return Math.min(backoff, maxBackoff);
    }

    /**
     * Move exhausted retries to dead letter queue.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void processDeadLetterQueue() {
        try {
            List<Job> deadLetterJobs = jobRepository.findDeadLetterJobs();

            if (deadLetterJobs.isEmpty()) {
                return;
            }

            log.warn("Moving {} jobs to dead letter queue", deadLetterJobs.size());

            for (Job job : deadLetterJobs) {
                job.setStatus(JobStatus.DEAD_LETTER);
                jobRepository.save(job);
            }

        } catch (Exception e) {
            log.error("Error processing dead letter queue", e);
        }
    }
}
