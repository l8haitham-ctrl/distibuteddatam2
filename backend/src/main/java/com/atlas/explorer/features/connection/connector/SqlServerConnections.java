package com.atlas.explorer.features.connection.connector;

import com.atlas.explorer.shared.domain.ExplorerException;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import java.sql.*;
import java.util.*;

public final class SqlServerConnections {
    private SqlServerConnections() {}
    public static Connection open(ConnectionConfig config) throws SQLException {
        // Set properties individually: user input can never add JDBC URL options.
        var source = new SQLServerDataSource();
        source.setServerName(config.host()); source.setPortNumber(config.port());
        source.setDatabaseName(config.database()); source.setUser(config.username());
        source.setPassword(config.password()); source.setEncrypt("true");
        source.setTrustServerCertificate(Boolean.TRUE.equals(config.trustServerCertificate()));
        source.setLoginTimeout(5); source.setQueryTimeout(10); source.setSocketTimeout(15000);
        return source.getConnection();
    }
    public static List<Map<String, Object>> query(Connection connection, String sql, Object... parameters) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(10);
            for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
            try (var result = statement.executeQuery()) {
                var metadata = result.getMetaData();
                var rows = new ArrayList<Map<String, Object>>();
                while (result.next()) {
                    var row = new LinkedHashMap<String, Object>();
                    for (int i = 1; i <= metadata.getColumnCount(); i++) row.put(metadata.getColumnLabel(i), result.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }
    public static ExplorerException sanitized(SQLException exception) {
        var state = Objects.toString(exception.getSQLState(), "");
        if (state.equals("28000") || exception.getErrorCode() == 18456) return new ExplorerException(ExplorerException.Code.AUTHENTICATION);
        if (state.startsWith("08") || state.startsWith("HYT")) return new ExplorerException(ExplorerException.Code.UNAVAILABLE);
        return new ExplorerException(ExplorerException.Code.QUERY);
    }
}
