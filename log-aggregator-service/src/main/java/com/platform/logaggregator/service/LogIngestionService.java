package com.platform.logaggregator.service;

import com.platform.common.model.LogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogIngestionService {

    private final LogStorageService storageService;

    /**
     * Process incoming log entry.
     */
    public void ingest(LogEntry log) {
        try {
            // Validate log
            if (log == null || log.getMessage() == null) {
                log.warn("Received invalid log entry, skipping");
                return;
            }

            // Store log (will be batched and enriched)
            storageService.storeLog(log);

        } catch (Exception e) {
            log.error("Failed to ingest log: jobId={}",
                log != null ? log.getJobId() : "null", e);
        }
    }
}
