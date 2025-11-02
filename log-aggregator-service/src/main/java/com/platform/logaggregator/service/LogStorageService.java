package com.platform.logaggregator.service;

import com.platform.common.model.LogEntry;
import com.platform.logaggregator.repository.ClickHouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogStorageService {

    private final ClickHouseRepository repository;
    private final LogEnrichmentService enrichmentService;

    @Value("${clickhouse.batch-size}")
    private int batchSize;

    private final List<LogEntry> buffer = new ArrayList<>();
    private final Lock lock = new ReentrantLock();

    /**
     * Add log to buffer. Will auto-flush when batch size reached.
     */
    public void storeLog(LogEntry log) {
        lock.lock();
        try {
            // Enrich log
            LogEntry enriched = enrichmentService.enrich(log);

            // Add to buffer
            buffer.add(enriched);

            // Flush if batch size reached
            if (buffer.size() >= batchSize) {
                flush();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Scheduled flush - runs every 5 seconds.
     */
    @Scheduled(fixedDelayString = "${clickhouse.flush-interval}")
    public void scheduledFlush() {
        if (!buffer.isEmpty()) {
            lock.lock();
            try {
                flush();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Flush buffer to ClickHouse.
     */
    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        List<LogEntry> toWrite = new ArrayList<>(buffer);
        buffer.clear();

        try {
            repository.batchInsert(toWrite);
            log.debug("Flushed {} logs to ClickHouse", toWrite.size());
        } catch (Exception e) {
            log.error("Failed to flush logs to ClickHouse: count={}", toWrite.size(), e);
            // In production, you might want to write to a dead letter queue
        }
    }
}
