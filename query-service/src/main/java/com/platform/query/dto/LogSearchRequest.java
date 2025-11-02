package com.platform.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSearchRequest {

    private String jobId;              // Filter by job ID
    private String workerId;           // Filter by worker
    private String level;              // INFO, WARN, ERROR
    private String serviceName;        // orchestrator, worker, etc.
    private String searchQuery;        // Full-text search on message

    private Long startTime;            // Epoch millis
    private Long endTime;              // Epoch millis

    @Builder.Default
    private Integer limit = 100;       // Max results (default 100, max 1000)

    @Builder.Default
    private Integer offset = 0;        // Pagination offset
}
