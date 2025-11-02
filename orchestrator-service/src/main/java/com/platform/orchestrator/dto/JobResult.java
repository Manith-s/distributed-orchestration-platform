package com.platform.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing a job result received from workers via Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResult {

    private UUID jobId;
    private boolean success;
    private String result;
    private String errorMessage;
    private String workerId;
}
