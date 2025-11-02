package com.platform.worker.executor.task;

import com.platform.common.model.Job;

/**
 * Interface for all job task implementations
 */
public interface JobTask {
    /**
     * Execute the job task
     * @param job The job to execute
     * @throws Exception if execution fails
     */
    void execute(Job job) throws Exception;

    /**
     * Get the task type this implementation handles.
     */
    String getType();
}
