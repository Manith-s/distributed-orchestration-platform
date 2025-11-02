package com.platform.orchestrator.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Job name is required")
    @Size(min = 3, max = 255, message = "Job name must be 3-255 characters")
    private String name;

    @NotBlank(message = "Job type is required")
    @Pattern(regexp = "EMAIL|DATA_SYNC|REPORT|IMAGE_PROCESS",
             message = "Invalid job type")
    private String type;

    @NotBlank(message = "Payload is required")
    private String payload;

    @Min(0) @Max(10)
    @Builder.Default
    private Integer priority = 0;

    @Min(0) @Max(10)
    @Builder.Default
    private Integer maxRetries = 3;
}
