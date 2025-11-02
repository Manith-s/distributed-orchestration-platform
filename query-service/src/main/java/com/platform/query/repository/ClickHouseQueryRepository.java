package com.platform.query.repository;

import com.platform.query.dto.LogSearchRequest;
import com.platform.query.dto.LogSearchResponse;
import com.platform.query.dto.MetricsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ClickHouseQueryRepository {

    private final DataSource clickHouseDataSource;

    /**
     * Search logs with filters.
     */
    public LogSearchResponse searchLogs(LogSearchRequest request) {
        StringBuilder sql = new StringBuilder(
            "SELECT timestamp, job_id, worker_id, level, message, service_name, metadata " +
            "FROM job_logs WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        // Add filters
        if (request.getJobId() != null) {
            sql.append(" AND job_id = ?");
            params.add(request.getJobId());
        }

        if (request.getWorkerId() != null) {
            sql.append(" AND worker_id = ?");
            params.add(request.getWorkerId());
        }

        if (request.getLevel() != null) {
            sql.append(" AND level = ?");
            params.add(request.getLevel());
        }

        if (request.getServiceName() != null) {
            sql.append(" AND service_name = ?");
            params.add(request.getServiceName());
        }

        if (request.getSearchQuery() != null) {
            sql.append(" AND positionCaseInsensitive(message, ?) > 0");
            params.add(request.getSearchQuery());
        }

        if (request.getStartTime() != null) {
            sql.append(" AND timestamp >= ?");
            params.add(request.getStartTime());
        }

        if (request.getEndTime() != null) {
            sql.append(" AND timestamp <= ?");
            params.add(request.getEndTime());
        }

        sql.append(" ORDER BY timestamp DESC");
        sql.append(" LIMIT ? OFFSET ?");
        params.add(Math.min(request.getLimit(), 1000));  // Cap at 1000
        params.add(request.getOffset());

        List<LogSearchResponse.LogEntryDto> logs = new ArrayList<>();

        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(LogSearchResponse.LogEntryDto.builder()
                        .timestamp(rs.getLong("timestamp"))
                        .jobId(rs.getString("job_id"))
                        .workerId(rs.getString("worker_id"))
                        .level(rs.getString("level"))
                        .message(rs.getString("message"))
                        .serviceName(rs.getString("service_name"))
                        .metadata(rs.getString("metadata"))
                        .build());
                }
            }

        } catch (SQLException e) {
            log.error("Failed to search logs", e);
            throw new RuntimeException("Log search failed", e);
        }

        // Get total count (without limit/offset)
        Long totalCount = getTotalCount(request);

        return LogSearchResponse.builder()
            .logs(logs)
            .totalCount(totalCount)
            .limit(request.getLimit())
            .offset(request.getOffset())
            .build();
    }

    /**
     * Get total count for a search query.
     */
    private Long getTotalCount(LogSearchRequest request) {
        StringBuilder sql = new StringBuilder("SELECT count() FROM job_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // Same filters as search (without ORDER BY, LIMIT, OFFSET)
        if (request.getJobId() != null) {
            sql.append(" AND job_id = ?");
            params.add(request.getJobId());
        }

        if (request.getWorkerId() != null) {
            sql.append(" AND worker_id = ?");
            params.add(request.getWorkerId());
        }

        if (request.getLevel() != null) {
            sql.append(" AND level = ?");
            params.add(request.getLevel());
        }

        if (request.getServiceName() != null) {
            sql.append(" AND service_name = ?");
            params.add(request.getServiceName());
        }

        if (request.getSearchQuery() != null) {
            sql.append(" AND positionCaseInsensitive(message, ?) > 0");
            params.add(request.getSearchQuery());
        }

        if (request.getStartTime() != null) {
            sql.append(" AND timestamp >= ?");
            params.add(request.getStartTime());
        }

        if (request.getEndTime() != null) {
            sql.append(" AND timestamp <= ?");
            params.add(request.getEndTime());
        }

        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            log.error("Failed to get total count", e);
        }

        return 0L;
    }

    /**
     * Get metrics aggregation.
     */
    public MetricsResponse getMetrics(Long startTime, Long endTime) {
        Map<String, Long> logCountByLevel = new HashMap<>();
        Map<String, Long> logCountByService = new HashMap<>();
        Long totalLogs = 0L;
        List<MetricsResponse.TimeSeriesPoint> timeSeries = new ArrayList<>();

        String whereClause = buildTimeWhereClause(startTime, endTime);

        // Count by level
        String levelSql = "SELECT level, count() as cnt FROM job_logs " +
                         whereClause + " GROUP BY level";
        executeCountQuery(levelSql, startTime, endTime, logCountByLevel);

        // Count by service
        String serviceSql = "SELECT service_name, count() as cnt FROM job_logs " +
                           whereClause + " GROUP BY service_name";
        executeCountQuery(serviceSql, startTime, endTime, logCountByService);

        // Total count
        String totalSql = "SELECT count() FROM job_logs " + whereClause;
        totalLogs = executeTotalQuery(totalSql, startTime, endTime);

        // Time series (hourly buckets)
        String timeSeriesSql =
            "SELECT toStartOfHour(timestamp / 1000) * 1000 as time_bucket, count() as cnt " +
            "FROM job_logs " + whereClause +
            " GROUP BY time_bucket ORDER BY time_bucket";
        timeSeries = executeTimeSeriesQuery(timeSeriesSql, startTime, endTime);

        return MetricsResponse.builder()
            .logCountByLevel(logCountByLevel)
            .logCountByService(logCountByService)
            .totalLogs(totalLogs)
            .logVolumeOverTime(timeSeries)
            .build();
    }

    private String buildTimeWhereClause(Long startTime, Long endTime) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (startTime != null) {
            where.append(" AND timestamp >= ").append(startTime);
        }
        if (endTime != null) {
            where.append(" AND timestamp <= ").append(endTime);
        }
        return where.toString();
    }

    private void executeCountQuery(String sql, Long startTime, Long endTime,
                                   Map<String, Long> resultMap) {
        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                resultMap.put(rs.getString(1), rs.getLong(2));
            }

        } catch (SQLException e) {
            log.error("Failed to execute count query", e);
        }
    }

    private Long executeTotalQuery(String sql, Long startTime, Long endTime) {
        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (SQLException e) {
            log.error("Failed to execute total query", e);
        }
        return 0L;
    }

    private List<MetricsResponse.TimeSeriesPoint> executeTimeSeriesQuery(
            String sql, Long startTime, Long endTime) {
        List<MetricsResponse.TimeSeriesPoint> points = new ArrayList<>();

        try (Connection conn = clickHouseDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                points.add(MetricsResponse.TimeSeriesPoint.builder()
                    .timestamp(rs.getLong(1))
                    .count(rs.getLong(2))
                    .build());
            }

        } catch (SQLException e) {
            log.error("Failed to execute time series query", e);
        }

        return points;
    }
}
