package com.platform.orchestrator.service;

import com.platform.common.model.Job;
import com.platform.common.model.JobStatus;
import com.platform.orchestrator.dto.JobRequest;
import com.platform.orchestrator.dto.JobResponse;
import com.platform.orchestrator.exception.JobNotFoundException;
import com.platform.orchestrator.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceUnitTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private JobService jobService;

    private JobRequest testJobRequest;
    private Job testJob;

    @BeforeEach
    void setUp() {
        testJobRequest = new JobRequest();
        testJobRequest.setName("Test Email Job");
        testJobRequest.setType("EMAIL");
        testJobRequest.setPayload("{\"to\":\"test@example.com\"}");
        testJobRequest.setPriority(5);
        testJobRequest.setMaxRetries(3);

        testJob = Job.builder()
                .id(UUID.randomUUID())
                .name("Test Email Job")
                .type("EMAIL")
                .payload("{\"to\":\"test@example.com\"}")
                .status(JobStatus.PENDING)
                .priority(5)
                .maxRetries(3)
                .retryCount(0)
                .build();
    }

    @Test
    void submitJob_ValidRequest_SavesAndReturnsJobResponse() {
        // Given
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // When
        JobResponse result = jobService.submitJob(testJobRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testJob.getId());
        assertThat(result.getName()).isEqualTo("Test Email Job");
        assertThat(result.getType()).isEqualTo("EMAIL");
        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(result.getPriority()).isEqualTo(5);
        assertThat(result.getMaxRetries()).isEqualTo(3);

        verify(jobRepository).save(any(Job.class));
        verify(metricsService).recordJobSubmission("EMAIL");
    }

    @Test
    void getJob_ExistingJob_ReturnsJobResponse() {
        // Given
        UUID jobId = testJob.getId();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));

        // When
        JobResponse result = jobService.getJob(jobId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(jobId);
        assertThat(result.getName()).isEqualTo("Test Email Job");
        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);

        verify(jobRepository).findById(jobId);
    }

    @Test
    void getJob_NonExistingJob_ThrowsJobNotFoundException() {
        // Given
        UUID nonExistingId = UUID.randomUUID();
        when(jobRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> jobService.getJob(nonExistingId))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job not found: " + nonExistingId);

        verify(jobRepository).findById(nonExistingId);
    }

    @Test
    void getStatistics_ReturnsJobCountsByStatus() {
        // Given
        when(jobRepository.count()).thenReturn(100L);
        when(jobRepository.countByStatus(JobStatus.PENDING)).thenReturn(25L);
        when(jobRepository.countByStatus(JobStatus.QUEUED)).thenReturn(10L);
        when(jobRepository.countByStatus(JobStatus.RUNNING)).thenReturn(15L);
        when(jobRepository.countByStatus(JobStatus.COMPLETED)).thenReturn(40L);
        when(jobRepository.countByStatus(JobStatus.FAILED)).thenReturn(10L);

        // When
        var stats = jobService.getStatistics();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("total")).isEqualTo(100L);
        assertThat(stats.get("pending")).isEqualTo(25L);
        assertThat(stats.get("queued")).isEqualTo(10L);
        assertThat(stats.get("running")).isEqualTo(15L);
        assertThat(stats.get("completed")).isEqualTo(40L);
        assertThat(stats.get("failed")).isEqualTo(10L);

        verify(jobRepository).count();
        verify(jobRepository).countByStatus(JobStatus.PENDING);
        verify(jobRepository).countByStatus(JobStatus.QUEUED);
        verify(jobRepository).countByStatus(JobStatus.RUNNING);
        verify(jobRepository).countByStatus(JobStatus.COMPLETED);
        verify(jobRepository).countByStatus(JobStatus.FAILED);
    }
}
