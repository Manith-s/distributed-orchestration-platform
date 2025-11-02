package com.platform.query.service;

import com.platform.query.dto.MetricsResponse;
import com.platform.query.repository.ClickHouseQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

    private final ClickHouseQueryRepository repository;

    public MetricsResponse getMetrics(Long startTime, Long endTime) {
        log.info("Getting metrics: startTime={}, endTime={}", startTime, endTime);
        return repository.getMetrics(startTime, endTime);
    }

    public MetricsResponse getLast24HoursMetrics() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (24 * 60 * 60 * 1000);
        return repository.getMetrics(startTime, endTime);
    }
}
