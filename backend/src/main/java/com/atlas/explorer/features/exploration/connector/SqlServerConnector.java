package com.atlas.explorer.features.exploration.connector;

import com.atlas.explorer.shared.domain.ExplorerException;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import org.springframework.stereotype.Component;
import java.sql.*;
import java.util.*;
import static com.atlas.explorer.features.connection.connector.SqlServerConnections.*;

@Component
public class SqlServerConnector implements SchemaConnector {
    @FunctionalInterface private interface Operation<T> { T execute(Connection connection) throws SQLException; }
    private <T> T read(ConnectionConfig config, Operation<T> operation) {
        try (var connection = open(config)) { return operation.execute(connection); }
        catch (SQLException exception) { throw sanitized(exception); }
    }
    public String engine() { return "sqlserver"; }
    public List<Map<String, Object>> tables(ConnectionConfig config) { return read(config, this::tables); }
    public Map<String, Object> describe(ConnectionConfig config, String schema, String table) {
        return read(config, c -> describe(c, schema, table));
    }
    public List<Map<String, Object>> explore(ConnectionConfig config) {
        return read(config, c -> {
            var result = new ArrayList<Map<String, Object>>();
            for (var table : tables(c)) result.add(describe(c, (String)table.get("schema_name"), (String)table.get("table_name")));
            return result;
        });
    }
    private List<Map<String, Object>> tables(Connection c) throws SQLException {
        return query(c, """
            SELECT s.name AS schema_name, t.name AS table_name FROM sys.tables t
            JOIN sys.schemas s ON s.schema_id = t.schema_id
            WHERE t.is_ms_shipped = 0 ORDER BY s.name, t.name
            """);
    }
    private Map<String, Object> describe(Connection c, String schema, String table) throws SQLException {
        var found = query(c, """
            SELECT t.object_id FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
            WHERE s.name = ? AND t.name = ? AND t.is_ms_shipped = 0
            """, schema, table);
        if (found.isEmpty()) throw new ExplorerException(ExplorerException.Code.TABLE_NOT_FOUND);
        var id = found.getFirst().get("object_id");
        var columns = query(c, """
            SELECT c.name AS name, ty.name AS data_type, c.column_id AS position,
                c.max_length AS max_length_bytes, c.precision, c.scale,
                c.is_nullable AS nullable, c.is_identity AS is_identity, dc.definition AS default_value
            FROM sys.columns c JOIN sys.types ty ON ty.user_type_id = c.user_type_id
            LEFT JOIN sys.default_constraints dc ON dc.object_id = c.default_object_id
            WHERE c.object_id = ? ORDER BY c.column_id
            """, id);
        var primary = query(c, """
            SELECT kc.name AS constraint_name, c.name AS column_name, ic.key_ordinal AS position
            FROM sys.key_constraints kc
            JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
            JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
            WHERE kc.parent_object_id = ? AND kc.type = 'PK' ORDER BY ic.key_ordinal
            """, id);
        var foreign = query(c, """
            SELECT fk.name AS constraint_name, pc.name AS column_name, rs.name AS referenced_schema,
                rt.name AS referenced_table, rc.name AS referenced_column, fkc.constraint_column_id AS position
            FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
            JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
            JOIN sys.tables rt ON rt.object_id = fkc.referenced_object_id
            JOIN sys.schemas rs ON rs.schema_id = rt.schema_id
            JOIN sys.columns rc ON rc.object_id = rt.object_id AND rc.column_id = fkc.referenced_column_id
            WHERE fk.parent_object_id = ? ORDER BY fk.name, fkc.constraint_column_id
            """, id);
        return Map.of("schema_name", schema, "table_name", table, "columns", columns, "primary_key", primary, "foreign_keys", foreign);
    }
}
