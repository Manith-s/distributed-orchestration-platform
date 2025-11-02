package com.platform.query.controller;

import com.platform.query.dto.MetricsResponse;
import com.platform.query.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/metrics")
@Slf4j
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping
    public ResponseEntity<MetricsResponse> getMetrics(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        log.debug("Getting metrics: start={}, end={}", startTime, endTime);

        MetricsResponse response;
        if (startTime == null && endTime == null) {
            response = metricsService.getLast24HoursMetrics();
        } else {
            response = metricsService.getMetrics(startTime, endTime);
        }

        return ResponseEntity.ok(response);
    }
}
