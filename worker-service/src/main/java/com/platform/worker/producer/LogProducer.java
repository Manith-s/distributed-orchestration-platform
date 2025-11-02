package com.platform.worker.producer;

import com.platform.common.model.LogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogProducer {

    private final KafkaTemplate kafkaTemplate;

    @Value("${worker.kafka.topics.job-logs}")
    private String logsTopic;

    public void sendLog(UUID jobId, String level, String message, String workerId) {
        LogEntry entry = LogEntry.builder()
            .timestamp(Instant.now())
            .jobId(jobId)
            .workerId(workerId)
            .level(level)
            .message(message)
            .serviceName("worker-service")
            .metadata(Map.of())
            .build();

        kafkaTemplate.send(logsTopic, jobId.toString(), entry);
    }
}
