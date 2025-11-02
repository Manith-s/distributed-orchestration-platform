package com.platform.orchestrator.service;

import com.platform.common.model.JobStatus;
import com.platform.orchestrator.repository.JobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final JobRepository jobRepository;

    /**
     * Initialize metrics gauges.
     */
    @PostConstruct
    public void initMetrics() {
        // Register gauges for each job status
        Arrays.stream(JobStatus.values()).forEach(status -> {
            meterRegistry.gauge("jobs_by_status",
                    List.of(Tag.of("status", status.name())),
                    this,
                    service -> jobRepository.countByStatus(status));
        });

        // Total jobs gauge
        meterRegistry.gauge("jobs_total", this,
                service -> jobRepository.count());

        log.info("Metrics initialized");
    }

    /**
     * Update metrics periodically.
     * This is a no-op since gauges auto-update, but can be used for custom metrics.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void updateMetrics() {
        log.debug("Metrics updated - Total jobs: {}", jobRepository.count());
    }

    /**
     * Record job submission.
     */
    public void recordJobSubmission(String jobType) {
        meterRegistry.counter("jobs_submitted",
                "type", jobType).increment();
    }

    /**
     * Record job queued.
     */
    public void recordJobQueued(String jobType) {
        meterRegistry.counter("jobs_queued",
                "type", jobType).increment();
    }

    /**
     * Record job completion.
     */
    public void recordJobCompletion(String jobType, boolean success) {
        meterRegistry.counter("jobs_completed",
                "type", jobType,
                "success", String.valueOf(success)).increment();
    }

    /**
     * Record job retry.
     */
    public void recordJobRetry(String jobType) {
        meterRegistry.counter("jobs_retried",
                "type", jobType).increment();
    }
}
