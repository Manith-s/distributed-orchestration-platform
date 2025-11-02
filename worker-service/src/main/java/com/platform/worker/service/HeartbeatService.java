package com.platform.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class HeartbeatService {

    private final StringRedisTemplate redisTemplate;

    @Value("${worker.id}")
    private String workerId;

    @Scheduled(fixedDelayString = "${worker.heartbeat.interval}")
    public void sendHeartbeat() {
        String key = "worker:heartbeat:" + workerId;
        String timestamp = Instant.now().toString();

        redisTemplate.opsForValue().set(key, timestamp, Duration.ofSeconds(30));
        log.debug("Heartbeat sent: worker={}, time={}", workerId, timestamp);
    }
}
