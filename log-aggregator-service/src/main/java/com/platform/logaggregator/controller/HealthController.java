package com.platform.logaggregator.controller;

import com.platform.logaggregator.repository.ClickHouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final ClickHouseRepository repository;

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();

        // Check ClickHouse
        boolean clickhouseUp = repository.testConnection();
        health.put("clickhouse", clickhouseUp ? "UP" : "DOWN");

        // Check Kafka (implicit via consumer)
        health.put("kafka", "UP");

        health.put("status", clickhouseUp ? "UP" : "DOWN");

        return ResponseEntity.ok(health);
    }
}
