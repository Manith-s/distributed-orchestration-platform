package com.platform.orchestrator.repository;

import com.platform.common.model.Job;
import com.platform.common.model.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Page<Job> findByType(String type, Pageable pageable);

    long countByStatus(JobStatus status);

    /**
     * Find pending jobs ordered by priority (highest first).
     */
    @Query("SELECT j FROM Job j WHERE j.status = 'PENDING' " +
           "ORDER BY j.priority DESC, j.createdAt ASC")
    List<Job> findPendingJobsByPriority(Pageable pageable);

    /**
     * Find failed jobs eligible for retry.
     */
    @Query("SELECT j FROM Job j WHERE j.status = 'FAILED' " +
           "AND j.retryCount < j.maxRetries " +
           "AND j.completedAt < :threshold")
    List<Job> findJobsForRetry(@Param("threshold") LocalDateTime threshold);

    /**
     * Find jobs that exceeded max retries.
     */
    @Query("SELECT j FROM Job j WHERE j.status = 'FAILED' " +
           "AND j.retryCount >= j.maxRetries")
    List<Job> findDeadLetterJobs();
}
