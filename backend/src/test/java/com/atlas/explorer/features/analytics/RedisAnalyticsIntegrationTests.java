package com.atlas.explorer.features.analytics;

import com.atlas.explorer.features.catalog.config.CatalogProperties;
import com.atlas.explorer.features.catalog.connector.TrinoConnections;
import com.atlas.explorer.features.sql.dto.SqlRequest;
import com.atlas.explorer.features.sql.service.SqlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Real Redis/Hive/MinIO queries. Requires the documented PostgreSQL demo extraction. */
@EnabledIfEnvironmentVariable(named = "RUN_ANALYTICS_TESTS", matches = "true")
class RedisAnalyticsIntegrationTests {
    private final TrinoConnections connections = new TrinoConnections(new CatalogProperties(
        System.getenv("TRINO_JDBC_URL"), System.getenv("TRINO_REGISTRAR_PASSWORD"),
        System.getenv("TRINO_READER_PASSWORD"), ""));
    private final SqlService sql = new SqlService(connections);

    private List<?> first(String query) {
        var result = sql.query(new SqlRequest(query, 10));
        assertEquals(false, result.get("truncated"));
        return (List<?>) ((List<?>) result.get("rows")).getFirst();
    }

    @Test void redisHashesAreMappedToTypedRows() {
        var row = first("SELECT COUNT(*), SUM(loyalty_points), COUNT_IF(segment='vip') FROM redis.analytics.customer_segments");
        assertEquals(1000L, ((Number) row.get(0)).longValue());
        assertEquals(5005000L, ((Number) row.get(1)).longValue());
        assertEquals(200L, ((Number) row.get(2)).longValue());
        var customer = first("SELECT customer_id, segment, loyalty_points FROM redis.analytics.customer_segments WHERE customer_id=5");
        assertEquals(5L, ((Number) customer.get(0)).longValue());
        assertEquals("vip", customer.get(1));
        assertEquals(50L, ((Number) customer.get(2)).longValue());
    }

    @Test void minioParquetRemainsQueryable() {
        var row = first("SELECT COUNT(*), SUM(amount) FROM hive.postgresql_public.invoice");
        assertEquals(5000L, ((Number) row.get(0)).longValue());
        assertEquals(0, new BigDecimal("1750250.00").compareTo(new BigDecimal(row.get(1).toString())));
    }

    @Test void federatedJoinPreservesInvoicesAndMatchesAllCustomers() {
        var row = first("SELECT COUNT(*), COUNT(DISTINCT r.customer_id), SUM(i.amount), COUNT_IF(r.segment='vip') "
            + "FROM hive.postgresql_public.invoice i JOIN redis.analytics.customer_segments r ON i.customer_id=r.customer_id");
        assertEquals(5000L, ((Number) row.get(0)).longValue());
        assertEquals(1000L, ((Number) row.get(1)).longValue());
        assertEquals(0, new BigDecimal("1750250.00").compareTo(new BigDecimal(row.get(2).toString())));
        assertEquals(1000L, ((Number) row.get(3)).longValue());
    }

    @Test void readerCannotWriteThroughTrino() throws Exception {
        try (var connection = connections.open(false); var statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                "CREATE TABLE redis.analytics.forbidden_write (customer_id BIGINT)"));
        }
        assertEquals(1000L, ((Number) first("SELECT COUNT(*) FROM redis.analytics.customer_segments").getFirst()).longValue());
    }

    @Test void hiveRegistrarHasNoRedisAccess() throws Exception {
        try (var connection = connections.open(true); var statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            assertThrows(SQLException.class, () -> statement.executeQuery("SELECT * FROM redis.analytics.customer_segments LIMIT 1"));
        }
    }
}
