package com.platform.orchestrator.service;

import com.platform.common.model.Job;
import com.platform.common.model.JobStatus;
import com.platform.orchestrator.dto.JobRequest;
import com.platform.orchestrator.dto.JobResponse;
import com.platform.orchestrator.exception.JobNotFoundException;
import com.platform.orchestrator.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final MetricsService metricsService;

    /**
     * Submit a new job.
     */
    @Transactional
    public JobResponse submitJob(JobRequest request) {
        log.info("Submitting job: name={}, type={}", request.getName(), request.getType());

        Job job = Job.builder()
            .name(request.getName())
            .type(request.getType())
            .status(JobStatus.PENDING)
            .payload(request.getPayload())
            .priority(request.getPriority())
            .maxRetries(request.getMaxRetries())
            .retryCount(0)
            .build();

        job = jobRepository.save(job);
        log.info("Job submitted: id={}", job.getId());

        // Record metrics
        metricsService.recordJobSubmission(job.getType());

        return mapToResponse(job);
    }

    /**
     * Get job by ID.
     */
    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));
        return mapToResponse(job);
    }

    /**
     * List all jobs.
     */
    @Transactional(readOnly = true)
    public Page<JobResponse> listJobs(JobStatus status, String type, Pageable pageable) {
        Page<Job> jobs;

        if (status != null) {
            jobs = jobRepository.findByStatus(status, pageable);
        } else if (type != null) {
            jobs = jobRepository.findByType(type, pageable);
        } else {
            jobs = jobRepository.findAll(pageable);
        }

        return jobs.map(this::mapToResponse);
    }

    /**
     * Get job statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        return Map.of(
            "total", jobRepository.count(),
            "pending", jobRepository.countByStatus(JobStatus.PENDING),
            "queued", jobRepository.countByStatus(JobStatus.QUEUED),
            "running", jobRepository.countByStatus(JobStatus.RUNNING),
            "completed", jobRepository.countByStatus(JobStatus.COMPLETED),
            "failed", jobRepository.countByStatus(JobStatus.FAILED)
        );
    }

    /**
     * Map Job entity to response DTO.
     */
    private JobResponse mapToResponse(Job job) {
        return JobResponse.builder()
            .id(job.getId())
            .name(job.getName())
            .type(job.getType())
            .status(job.getStatus())
            .payload(job.getPayload())
            .priority(job.getPriority())
            .retryCount(job.getRetryCount())
            .maxRetries(job.getMaxRetries())
            .createdAt(job.getCreatedAt())
            .startedAt(job.getStartedAt())
            .completedAt(job.getCompletedAt())
            .errorMessage(job.getErrorMessage())
            .workerId(job.getWorkerId())
            .build();
    }
}
