package com.platform.query.service;

import com.platform.query.dto.LogSearchRequest;
import com.platform.query.dto.LogSearchResponse;
import com.platform.query.repository.ClickHouseQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogQueryService {

    private final ClickHouseQueryRepository repository;

    public LogSearchResponse searchLogs(LogSearchRequest request) {
        log.info("Searching logs: jobId={}, level={}, query={}",
            request.getJobId(), request.getLevel(), request.getSearchQuery());

        return repository.searchLogs(request);
    }

    public LogSearchResponse getJobLogs(String jobId) {
        LogSearchRequest request = LogSearchRequest.builder()
            .jobId(jobId)
            .limit(1000)
            .build();

        return repository.searchLogs(request);
    }
}
