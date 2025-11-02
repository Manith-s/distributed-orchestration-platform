package com.platform.orchestrator.controller;

import com.platform.common.model.JobStatus;
import com.platform.orchestrator.dto.JobRequest;
import com.platform.orchestrator.dto.JobResponse;
import com.platform.orchestrator.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Job Management", description = "APIs for job submission, retrieval, and monitoring")
public class JobController {

    private final JobService jobService;

    @Operation(
        summary = "Submit a new job",
        description = "Creates and submits a new job for execution. The job will be queued and distributed to available worker nodes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Job created successfully",
            content = @Content(schema = @Schema(implementation = JobResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid job request"
        )
    })
    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody JobRequest request) {
        JobResponse response = jobService.submitJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Get job by ID",
        description = "Retrieves detailed information about a specific job including its status, results, and execution history."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Job found",
            content = @Content(schema = @Schema(implementation = JobResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Job not found"
        )
    })
    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJob(
        @Parameter(description = "Unique job identifier") @PathVariable UUID jobId) {
        JobResponse response = jobService.getJob(jobId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "List all jobs",
        description = "Retrieves a paginated list of jobs with optional filtering by status and type."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved job list"
        )
    })
    @GetMapping
    public ResponseEntity<Page<JobResponse>> listJobs(
            @Parameter(description = "Filter by job status") @RequestParam(required = false) JobStatus status,
            @Parameter(description = "Filter by job type") @RequestParam(required = false) String type,
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable) {

        Page<JobResponse> jobs = jobService.listJobs(status, type, pageable);
        return ResponseEntity.ok(jobs);
    }

    @Operation(
        summary = "Get job statistics",
        description = "Retrieves aggregated statistics about job execution including counts by status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved statistics"
        )
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        Map<String, Long> stats = jobService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}
