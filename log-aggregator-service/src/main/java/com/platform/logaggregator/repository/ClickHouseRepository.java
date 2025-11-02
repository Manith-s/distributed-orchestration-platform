package com.platform.logaggregator.repository;

import com.platform.common.model.LogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ClickHouseRepository {

    private final DataSource clickHouseDataSource;

    private static final String INSERT_SQL =
        "INSERT INTO job_logs (timestamp, job_id, worker_id, level, message, metadata, service_name) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    /**
     * Batch insert logs into ClickHouse.
     */
    public void batchInsert(List<LogEntry> logs) {
        if (logs.isEmpty()) {
            return;
        }

        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {

            for (LogEntry log : logs) {
                stmt.setLong(1, log.getTimestamp().toEpochMilli());
                stmt.setString(2, log.getJobId() != null ? log.getJobId().toString() : null);
                stmt.setString(3, log.getWorkerId());
                stmt.setString(4, log.getLevel());
                stmt.setString(5, log.getMessage());
                stmt.setString(6, log.getMetadata() != null ? log.getMetadata().toString() : "{}");
                stmt.setString(7, log.getServiceName());
                stmt.addBatch();
            }

            stmt.executeBatch();
            log.info("Batch inserted {} logs to ClickHouse", logs.size());

        } catch (SQLException e) {
            log.error("Failed to batch insert logs to ClickHouse", e);
            throw new RuntimeException("ClickHouse batch insert failed", e);
        }
    }

    /**
     * Test connection to ClickHouse.
     */
    public boolean testConnection() {
        try (Connection conn = clickHouseDataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            log.error("ClickHouse connection test failed", e);
            return false;
        }
    }
}
