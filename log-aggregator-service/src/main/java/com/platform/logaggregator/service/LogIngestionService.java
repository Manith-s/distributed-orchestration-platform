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
    public void ingest(LogEntry logEntry) {
        try {
            // Validate log
            if (logEntry == null || logEntry.getMessage() == null) {
                log.warn("Received invalid log entry, skipping");
                return;
            }

            // Store log (will be batched and enriched)
            storageService.storeLog(logEntry);

        } catch (Exception e) {
            log.error("Failed to ingest log: jobId={}",
                logEntry != null ? logEntry.getJobId() : "null", e);
        }
    }
}
