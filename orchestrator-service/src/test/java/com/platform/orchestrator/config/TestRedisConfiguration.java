package com.platform.orchestrator.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * Test configuration that starts an embedded Redis server for integration tests.
 */
@TestConfiguration
@Profile("test")
@Slf4j
public class TestRedisConfiguration {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        try {
            redisServer = new RedisServer(6379);
            redisServer.start();
            log.info("Embedded Redis server started on port 6379");
        } catch (Exception e) {
            log.warn("Failed to start embedded Redis server: {}. Tests may fail if Redis connection is required.", e.getMessage());
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            log.info("Embedded Redis server stopped");
        }
    }
}
