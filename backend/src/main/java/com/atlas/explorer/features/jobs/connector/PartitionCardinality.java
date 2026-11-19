package com.atlas.explorer.features.jobs.connector;

import com.atlas.explorer.features.connection.connector.SqlServerConnections;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import java.sql.SQLException;

public class PartitionCardinality {
    public long distinct(ConnectionConfig source,String schema,String table,String column) {
        // Identifiers have already been resolved against source metadata by JobService.
        String sql="SELECT COUNT_BIG(DISTINCT "+quote(column)+") AS cardinality FROM "+quote(schema)+"."+quote(table);
        try(var connection=SqlServerConnections.open(source)) {
            return ((Number)SqlServerConnections.query(connection,sql).getFirst().get("cardinality")).longValue();
        } catch(SQLException exception) { throw SqlServerConnections.sanitized(exception); }
    }
    private String quote(String identifier) { return "["+identifier.replace("]","]]")+"]"; }
}
