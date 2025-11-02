package com.platform.worker.executor;

import com.platform.worker.executor.task.JobTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TaskRegistry {

    private final Map<String, JobTask> tasks = new HashMap<>();

    public TaskRegistry(List<JobTask> taskList) {
        for (JobTask task : taskList) {
            tasks.put(task.getType(), task);
            log.info("Registered task: type={}, class={}",
                task.getType(), task.getClass().getSimpleName());
        }
    }

    public JobTask getTask(String type) {
        JobTask task = tasks.get(type);
        if (task == null) {
            throw new IllegalArgumentException("Unknown task type: " + type);
        }
        return task;
    }
}
