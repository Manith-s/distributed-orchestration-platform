package com.platform.query.controller;

import com.platform.query.dto.LogSearchRequest;
import com.platform.query.dto.LogSearchResponse;
import com.platform.query.service.LogQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
@Slf4j
@RequiredArgsConstructor
public class LogSearchController {

    private final LogQueryService logQueryService;

    @PostMapping("/search")
    public ResponseEntity<LogSearchResponse> searchLogs(@RequestBody LogSearchRequest request) {
        log.debug("Search request: {}", request);
        LogSearchResponse response = logQueryService.searchLogs(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<LogSearchResponse> getJobLogs(@PathVariable String jobId) {
        log.debug("Getting logs for job: {}", jobId);
        LogSearchResponse response = logQueryService.getJobLogs(jobId);
        return ResponseEntity.ok(response);
    }
}
