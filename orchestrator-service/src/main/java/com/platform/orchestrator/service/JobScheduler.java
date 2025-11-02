package com.platform.orchestrator.service;

import com.platform.common.model.Job;
import com.platform.orchestrator.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobScheduler {

    private final JobRepository jobRepository;
    private final JobDistributionService distributionService;

    private static final int BATCH_SIZE = 100;

    /**
     * Poll for pending jobs and distribute them to workers.
     * Runs every 5 seconds (configured in application.yml).
     */
    @Scheduled(fixedDelayString = "${orchestrator.scheduling.interval}")
    @Transactional
    public void schedulePendingJobs() {
        try {
            // Fetch pending jobs by priority
            List<Job> pendingJobs = jobRepository.findPendingJobsByPriority(
                    PageRequest.of(0, BATCH_SIZE)
            );

            if (pendingJobs.isEmpty()) {
                log.debug("No pending jobs to schedule");
                return;
            }

            log.info("Scheduling {} pending jobs", pendingJobs.size());

            // Distribute each job
            for (Job job : pendingJobs) {
                try {
                    distributionService.distributeJob(job);
                } catch (Exception e) {
                    log.error("Failed to schedule job: id={}", job.getId(), e);
                    job.markAsFailed("Scheduling failed: " + e.getMessage());
                    jobRepository.save(job);
                }
            }

        } catch (Exception e) {
            log.error("Error in job scheduling", e);
        }
    }
}
