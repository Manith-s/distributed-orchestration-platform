package com.platform.worker.executor.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReportTask implements JobTask {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(Job job) throws Exception {
        log.info("Executing REPORT task: jobId={}", job.getId());

        JsonNode payload = objectMapper.readTree(job.getPayload());
        String reportType = payload.get("reportType").asText();

        log.info("Generating report: type={}", reportType);

        // Simulate report generation
        Thread.sleep(5000);  // 5 second delay

        log.info("Report generated: jobId={}", job.getId());
    }

    @Override
    public String getType() {
        return "REPORT";
    }
}
