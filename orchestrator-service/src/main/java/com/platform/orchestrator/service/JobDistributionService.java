package com.platform.orchestrator.service;

import com.platform.common.model.Job;
import com.platform.common.model.JobStatus;
import com.platform.orchestrator.dto.JobTask;
import com.platform.orchestrator.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobDistributionService {

    private final KafkaTemplate<String, JobTask> kafkaTemplate;
    private final JobRepository jobRepository;
    private final MetricsService metricsService;

    @Value("${orchestrator.kafka.topics.job-tasks}")
    private String jobTasksTopic;

    /**
     * Distribute a job to workers via Kafka.
     */
    @Transactional
    public void distributeJob(Job job) {
        try {
            // Update job status to QUEUED
            job.setStatus(JobStatus.QUEUED);
            jobRepository.save(job);

            // Record metrics
            metricsService.recordJobQueued(job.getType());

            // Create job task
            JobTask task = JobTask.builder()
                    .jobId(job.getId())
                    .type(job.getType())
                    .payload(job.getPayload())
                    .retryCount(job.getRetryCount())
                    .maxRetries(job.getMaxRetries())
                    .build();

            // Send to Kafka
            kafkaTemplate.send(jobTasksTopic, job.getId().toString(), task)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send job to Kafka: jobId={}", job.getId(), ex);
                            handleDistributionFailure(job);
                        } else {
                            log.info("Job sent to Kafka: jobId={}, topic={}",
                                    job.getId(), jobTasksTopic);
                        }
                    });

        } catch (Exception e) {
            log.error("Error distributing job: jobId={}", job.getId(), e);
            handleDistributionFailure(job);
        }
    }

    /**
     * Handle job distribution failure.
     */
    @Transactional
    public void handleDistributionFailure(Job job) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage("Failed to distribute job to workers");
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
    }
}
