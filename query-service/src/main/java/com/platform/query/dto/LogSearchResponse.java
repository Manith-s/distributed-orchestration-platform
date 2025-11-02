package com.platform.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSearchResponse {

    private List<LogEntryDto> logs;
    private Long totalCount;
    private Integer limit;
    private Integer offset;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntryDto {
        private Long timestamp;
        private String jobId;
        private String workerId;
        private String level;
        private String message;
        private String serviceName;
        private String metadata;
    }
}
