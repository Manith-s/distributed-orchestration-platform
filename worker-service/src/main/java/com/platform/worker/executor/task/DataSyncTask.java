package com.platform.worker.executor.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataSyncTask implements JobTask {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(Job job) throws Exception {
        log.info("Executing DATA_SYNC task: jobId={}", job.getId());

        JsonNode payload = objectMapper.readTree(job.getPayload());
        String apiUrl = payload.get("apiUrl").asText();
        String entity = payload.has("entity") ? payload.get("entity").asText() : "unknown";

        log.info("Syncing data: apiUrl={}, entity={}", apiUrl, entity);

        // Simulate API call and data sync
        Thread.sleep(3000);  // 3 second delay

        log.info("Data sync completed: jobId={}", job.getId());
    }

    @Override
    public String getType() {
        return "DATA_SYNC";
    }
}
