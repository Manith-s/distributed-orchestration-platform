package com.platform.logaggregator.consumer;

import com.platform.common.model.LogEntry;
import com.platform.logaggregator.service.LogIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogConsumer {

    private final LogIngestionService ingestionService;

    @KafkaListener(
        topics = "job.logs",
        groupId = "log-aggregator-group",
        concurrency = "5"
    )
    public void consumeLog(LogEntry log, Acknowledgment acknowledgment) {
        try {
            log.trace("Received log: jobId={}, level={}", log.getJobId(), log.getLevel());

            // Ingest log
            ingestionService.ingest(log);

            // Acknowledge
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error consuming log from Kafka", e);
            acknowledgment.acknowledge();  // Ack anyway to prevent redelivery loop
        }
    }
}
