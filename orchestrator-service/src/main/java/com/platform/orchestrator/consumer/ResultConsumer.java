package com.platform.orchestrator.consumer;

import com.platform.common.model.Job;
import com.platform.common.model.JobStatus;
import com.platform.orchestrator.dto.JobResult;
import com.platform.orchestrator.repository.JobRepository;
import com.platform.orchestrator.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResultConsumer {

    private final JobRepository jobRepository;
    private final MetricsService metricsService;

    /**
     * Listen for job results from workers.
     */
    @KafkaListener(
            topics = "${orchestrator.kafka.topics.job-results}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consumeResult(JobResult result) {
        try {
            log.info("Received job result: jobId={}, success={}",
                    result.getJobId(), result.isSuccess());

            Job job = jobRepository.findById(result.getJobId())
                    .orElseThrow(() -> new RuntimeException(
                            "Job not found: " + result.getJobId()));

            if (result.isSuccess()) {
                handleSuccess(job, result);
            } else {
                handleFailure(job, result);
            }

        } catch (Exception e) {
            log.error("Error processing job result: jobId={}",
                    result.getJobId(), e);
        }
    }

    /**
     * Handle successful job completion.
     */
    private void handleSuccess(Job job, JobResult result) {
        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setWorkerId(result.getWorkerId());
        job.setErrorMessage(null);

        jobRepository.save(job);

        // Record metrics
        metricsService.recordJobCompletion(job.getType(), true);

        log.info("Job completed successfully: jobId={}, workerId={}",
                job.getId(), result.getWorkerId());
    }

    /**
     * Handle failed job execution.
     */
    private void handleFailure(Job job, JobResult result) {
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        job.setWorkerId(result.getWorkerId());
        job.setErrorMessage(result.getErrorMessage());

        jobRepository.save(job);

        // Record metrics
        metricsService.recordJobCompletion(job.getType(), false);

        log.warn("Job failed: jobId={}, workerId={}, retryCount={}/{}, error={}",
                job.getId(), result.getWorkerId(), job.getRetryCount(),
                job.getMaxRetries(), result.getErrorMessage());

        // Retry service will pick this up if retries remain
        if (job.getRetryCount() >= job.getMaxRetries()) {
            log.error("Job exceeded max retries: jobId={}", job.getId());
        }
    }
}
