package com.platform.worker.executor;

import com.platform.common.model.Job;
import com.platform.common.model.JobStatus;
import com.platform.worker.dto.JobResult;
import com.platform.worker.executor.task.JobTask;
import com.platform.worker.producer.LogProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobExecutor {

    private final TaskRegistry taskRegistry;
    private final LogProducer logProducer;

    @Value("${worker.id}")
    private String workerId;

    public JobResult execute(Job job) {
        log.info("Starting job execution: jobId={}, type={}", job.getId(), job.getType());

        LocalDateTime startTime = LocalDateTime.now();
        logProducer.sendLog(job.getId(), "INFO", "Job execution started", workerId);

        try {
            // Get task implementation
            JobTask task = taskRegistry.getTask(job.getType());

            // Execute with timeout (10 minutes)
            executeWithTimeout(task, job, 600000);

            // Success
            LocalDateTime completedTime = LocalDateTime.now();
            logProducer.sendLog(job.getId(), "INFO", "Job completed successfully", workerId);

            return JobResult.builder()
                .jobId(job.getId())
                .status(JobStatus.COMPLETED)
                .startedAt(startTime)
                .completedAt(completedTime)
                .workerId(workerId)
                .build();

        } catch (TimeoutException e) {
            log.error("Job execution timeout: jobId={}", job.getId(), e);
            logProducer.sendLog(job.getId(), "ERROR", "Job timeout: " + e.getMessage(), workerId);

            return JobResult.builder()
                .jobId(job.getId())
                .status(JobStatus.FAILED)
                .startedAt(startTime)
                .completedAt(LocalDateTime.now())
                .errorMessage("Execution timeout")
                .workerId(workerId)
                .build();

        } catch (Exception e) {
            log.error("Job execution failed: jobId={}", job.getId(), e);
            logProducer.sendLog(job.getId(), "ERROR", "Job failed: " + e.getMessage(), workerId);

            return JobResult.builder()
                .jobId(job.getId())
                .status(JobStatus.FAILED)
                .startedAt(startTime)
                .completedAt(LocalDateTime.now())
                .errorMessage(e.getMessage())
                .workerId(workerId)
                .build();
        }
    }

    private void executeWithTimeout(JobTask task, Job job, long timeoutMs)
            throws Exception, TimeoutException {

        Thread executionThread = new Thread(() -> {
            try {
                task.execute(job);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        executionThread.start();
        executionThread.join(timeoutMs);

        if (executionThread.isAlive()) {
            executionThread.interrupt();
            throw new TimeoutException("Task execution exceeded timeout");
        }
    }
}
