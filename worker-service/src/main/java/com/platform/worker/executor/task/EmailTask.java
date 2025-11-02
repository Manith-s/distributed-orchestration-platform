package com.platform.worker.executor.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailTask implements JobTask {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(Job job) throws Exception {
        log.info("Executing EMAIL task: jobId={}", job.getId());

        // Parse payload
        JsonNode payload = objectMapper.readTree(job.getPayload());
        String to = payload.get("to").asText();
        String subject = payload.has("subject") ? payload.get("subject").asText() : "No Subject";
        String body = payload.has("body") ? payload.get("body").asText() : "";

        log.info("Sending email: to={}, subject={}", to, subject);

        // Simulate email sending
        Thread.sleep(2000);  // 2 second delay

        log.info("Email sent successfully: jobId={}", job.getId());
    }

    @Override
    public String getType() {
        return "EMAIL";
    }
}
