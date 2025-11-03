package com.platform.logaggregator.service;

import com.platform.common.model.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class LogEnrichmentService {

    private final String hostname;

    public LogEnrichmentService() {
        this.hostname = getHostname();
    }

    /**
     * Enrich log entry with additional metadata.
     */
    public LogEntry enrich(LogEntry log) {
        Map<String, String> metadata = new HashMap<>();

        // Add existing metadata
        if (log.getMetadata() != null) {
            metadata.putAll(log.getMetadata());
        }

        // Add enrichment fields
        metadata.put("aggregator_hostname", hostname);
        metadata.put("ingestion_time", String.valueOf(System.currentTimeMillis()));

        // Add environment (could come from config)
        metadata.put("environment", "development");

        log.setMetadata(metadata);
        return log;
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Could not determine hostname, using 'unknown'");
            return "unknown";
        }
    }
}
