package com.platform.worker.producer;

import com.platform.worker.dto.JobResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResultProducer {

    private final KafkaTemplate kafkaTemplate;

    @Value("${worker.kafka.topics.job-results}")
    private String resultsTopic;

    public void sendResult(JobResult result) {
        log.info("Sending result to orchestrator: jobId={}, status={}",
            result.getJobId(), result.getStatus());

        kafkaTemplate.send(resultsTopic, result.getJobId().toString(), result);
    }
}
