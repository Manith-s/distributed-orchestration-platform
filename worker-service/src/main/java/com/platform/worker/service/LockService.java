package com.platform.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LockService {

    private final StringRedisTemplate redisTemplate;

    @Value("${worker.id}")
    private String workerId;

    @Value("${worker.lock.timeout}")
    private long lockTimeoutMs;

    /**
     * Acquire a distributed lock for a job.
     */
    public boolean acquireLock(UUID jobId) {
        String key = "job:lock:" + jobId;
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(key, workerId, Duration.ofMillis(lockTimeoutMs));

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Lock acquired: jobId={}, worker={}", jobId, workerId);
            return true;
        } else {
            log.debug("Lock already held: jobId={}", jobId);
            return false;
        }
    }

    /**
     * Release a lock (only if this worker holds it).
     */
    public void releaseLock(UUID jobId) {
        String key = "job:lock:" + jobId;
        String holder = redisTemplate.opsForValue().get(key);

        if (workerId.equals(holder)) {
            redisTemplate.delete(key);
            log.debug("Lock released: jobId={}, worker={}", jobId, workerId);
        }
    }
}
