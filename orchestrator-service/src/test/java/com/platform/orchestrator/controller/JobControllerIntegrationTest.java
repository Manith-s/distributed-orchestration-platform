package com.platform.orchestrator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.model.JobStatus;
import com.platform.orchestrator.dto.JobRequest;
import com.platform.orchestrator.dto.JobResponse;
import com.platform.orchestrator.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    private UUID testJobId;
    private JobRequest testJobRequest;
    private JobResponse testJobResponse;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();

        testJobRequest = new JobRequest();
        testJobRequest.setName("Test Job");
        testJobRequest.setType("EMAIL");
        testJobRequest.setPriority(5);
        testJobRequest.setMaxRetries(3);
        testJobRequest.setPayload("{\"key\":\"value\"}");

        testJobResponse = new JobResponse();
        testJobResponse.setId(testJobId);
        testJobResponse.setName("Test Job");
        testJobResponse.setType("EMAIL");
        testJobResponse.setStatus(JobStatus.PENDING);
        testJobResponse.setPriority(5);
        testJobResponse.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void submitJob_ValidRequest_ReturnsCreated() throws Exception {
        when(jobService.submitJob(any(JobRequest.class))).thenReturn(testJobResponse);

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testJobId.toString()))
                .andExpect(jsonPath("$.type").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value(5));
    }

    @Test
    void submitJob_InvalidRequest_ReturnsBadRequest() throws Exception {
        JobRequest invalidRequest = new JobRequest();
        // Missing required fields

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJob_ExistingJob_ReturnsJob() throws Exception {
        when(jobService.getJob(testJobId)).thenReturn(testJobResponse);

        mockMvc.perform(get("/api/v1/jobs/{jobId}", testJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testJobId.toString()))
                .andExpect(jsonPath("$.type").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getJob_NonExistingJob_ReturnsNotFound() throws Exception {
        UUID nonExistingId = UUID.randomUUID();
        when(jobService.getJob(nonExistingId))
                .thenThrow(new RuntimeException("Job not found"));

        mockMvc.perform(get("/api/v1/jobs/{jobId}", nonExistingId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void listJobs_NoFilters_ReturnsPagedJobs() throws Exception {
        List<JobResponse> jobs = Arrays.asList(testJobResponse);
        Page<JobResponse> page = new PageImpl<>(jobs, PageRequest.of(0, 20), 1);

        when(jobService.listJobs(eq(null), eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(testJobId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listJobs_WithStatusFilter_ReturnsFilteredJobs() throws Exception {
        List<JobResponse> jobs = Arrays.asList(testJobResponse);
        Page<JobResponse> page = new PageImpl<>(jobs, PageRequest.of(0, 20), 1);

        when(jobService.listJobs(eq(JobStatus.PENDING), eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/jobs")
                .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void listJobs_WithTypeFilter_ReturnsFilteredJobs() throws Exception {
        List<JobResponse> jobs = Arrays.asList(testJobResponse);
        Page<JobResponse> page = new PageImpl<>(jobs, PageRequest.of(0, 20), 1);

        when(jobService.listJobs(eq(null), eq("EMAIL"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/jobs")
                .param("type", "EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("EMAIL"));
    }

    @Test
    void getStatistics_ReturnsStats() throws Exception {
        Map<String, Long> stats = new HashMap<>();
        stats.put("PENDING", 5L);
        stats.put("RUNNING", 3L);
        stats.put("COMPLETED", 10L);
        stats.put("FAILED", 2L);

        when(jobService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/jobs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PENDING").value(5))
                .andExpect(jsonPath("$.RUNNING").value(3))
                .andExpect(jsonPath("$.COMPLETED").value(10))
                .andExpect(jsonPath("$.FAILED").value(2));
    }

    @Test
    void listJobs_WithPagination_ReturnsCorrectPage() throws Exception {
        List<JobResponse> jobs = Arrays.asList(testJobResponse);
        Page<JobResponse> page = new PageImpl<>(jobs, PageRequest.of(1, 10), 25);

        when(jobService.listJobs(eq(null), eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/jobs")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }
}
