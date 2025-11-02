package com.platform.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResponse {

    private Map<String, Long> logCountByLevel;
    private Map<String, Long> logCountByService;
    private Long totalLogs;
    private List<TimeSeriesPoint> logVolumeOverTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private Long timestamp;
        private Long count;
    }
}
