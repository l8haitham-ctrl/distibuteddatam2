package com.atlas.explorer.features.jobs.connector;

import com.atlas.explorer.features.connection.connector.SqlServerConnections;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import java.sql.SQLException;

public class PartitionCardinality {
    private final com.atlas.explorer.features.connectors.connector.PluginRegistry plugins;
    public PartitionCardinality(){this(com.atlas.explorer.features.connectors.connector.PluginRegistry.discover());}
    public PartitionCardinality(com.atlas.explorer.features.connectors.connector.PluginRegistry plugins){this.plugins=plugins;}
    public long distinct(ConnectionConfig source,String schema,String table,String column) {
        // Identifiers have already been resolved against source metadata by JobService.
        var plugin=plugins.get(source.engine());
        String sql=plugin.distinctSql(schema,table,column);
        try(var connection=plugin.open(com.atlas.explorer.features.connection.connector.JdbcConnections.settings(source))) {
            return ((Number)SqlServerConnections.query(connection,sql).getFirst().get("cardinality")).longValue();
        } catch(SQLException exception) { throw com.atlas.explorer.features.connection.connector.JdbcConnections.sanitized(exception); }
    }
    private String quote(String identifier) { return "["+identifier.replace("]","]]")+"]"; }
}
