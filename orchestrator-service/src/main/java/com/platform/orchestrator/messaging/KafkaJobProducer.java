package com.platform.orchestrator.messaging;

import com.platform.common.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaJobProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendJob(String topic, String key, Job job) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, job);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Job sent to Kafka: jobId={}, partition={}",
                    job.getId(), result.getRecordMetadata().partition());
            } else {
                log.error("Failed to send job to Kafka: jobId={}", job.getId(), ex);
            }
        });
    }
}
