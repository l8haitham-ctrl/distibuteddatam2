package com.atlas.explorer.features.connection.connector;

import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import org.springframework.stereotype.Component;
import java.sql.SQLException;
import java.util.Map;

@Component
public class SqlServerConnectionConnector implements DatabaseConnector {
    public String engine() { return "sqlserver"; }
    public Map<String, Object> test(ConnectionConfig config) {
        try (var connection = SqlServerConnections.open(config)) {
            return SqlServerConnections.query(connection, "SELECT DB_NAME() AS database_name, CURRENT_USER AS database_user").getFirst();
        } catch (SQLException exception) { throw SqlServerConnections.sanitized(exception); }
    }
}
