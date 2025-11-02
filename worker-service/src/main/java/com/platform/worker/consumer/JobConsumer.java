package com.platform.worker.consumer;

import com.platform.common.model.Job;
import com.platform.worker.dto.JobResult;
import com.platform.worker.executor.JobExecutor;
import com.platform.worker.producer.ResultProducer;
import com.platform.worker.service.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobConsumer {

    private final JobExecutor jobExecutor;
    private final ResultProducer resultProducer;
    private final LockService lockService;

    @KafkaListener(
        topics = "${worker.kafka.topics.job-tasks}",
        groupId = "worker-group",
        concurrency = "3"
    )
    public void consumeJob(Job job, Acknowledgment acknowledgment) {
        log.info("Received job: id={}, type={}, priority={}",
            job.getId(), job.getType(), job.getPriority());

        // Try to acquire lock
        if (!lockService.acquireLock(job.getId())) {
            log.warn("Job already being processed: id={}", job.getId());
            acknowledgment.acknowledge();
            return;
        }

        try {
            // Execute job
            JobResult result = jobExecutor.execute(job);

            // Send result back to orchestrator
            resultProducer.sendResult(result);

            // Acknowledge Kafka message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing job: id={}", job.getId(), e);
            acknowledgment.acknowledge();
        } finally {
            // Release lock
            lockService.releaseLock(job.getId());
        }
    }
}
