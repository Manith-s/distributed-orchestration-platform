package com.platform.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing a job task to be sent to workers via Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTask {

    private UUID jobId;
    private String type;
    private String payload;
    private Integer retryCount;
    private Integer maxRetries;
}
