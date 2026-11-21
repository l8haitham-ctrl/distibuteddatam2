package com.atlas.explorer.features.sql.service;
import com.atlas.explorer.features.catalog.connector.TrinoConnections;
import com.atlas.explorer.features.sql.dto.SqlRequest;
import io.trino.jdbc.TrinoResultSet;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.*;
import java.sql.*;
@Service
@ConditionalOnProperty(name="catalog.enabled",havingValue="true")
public class SqlService {
    private final TrinoConnections connections;
    public SqlService(TrinoConnections connections){this.connections=connections;}
    public Map<String,Object> query(SqlRequest request) {
        String sql=validate(request.sql());long started=System.nanoTime();
        try(var connection=connections.open(false);var statement=connection.createStatement()) {
            statement.setQueryTimeout(60);statement.setMaxRows(request.maxRows()+1);
            try(var rs=statement.executeQuery(sql)) {
                var columns=new ArrayList<Map<String,Object>>();var metadata=rs.getMetaData();
                for(int i=1;i<=metadata.getColumnCount();i++)columns.add(Map.of("name",metadata.getColumnLabel(i),"type",metadata.getColumnTypeName(i)));
                var rows=new ArrayList<List<Object>>();boolean truncated=false;
                while(rs.next()) {
                    if(rows.size()==request.maxRows()){truncated=true;break;}
                    var row=new ArrayList<Object>();
                    for(int i=1;i<=metadata.getColumnCount();i++) {
                        Object value=rs.getObject(i);
                        row.add(value instanceof java.sql.Date || value instanceof Timestamp || value instanceof java.sql.Time?value.toString():value);
                    }
                    rows.add(row);
                }
                var trino=(TrinoResultSet)rs;var stats=trino.getStats();
                return Map.of("columns",columns,"rows",rows,"row_count",rows.size(),"truncated",truncated,
                    "execution_ms",(System.nanoTime()-started)/1_000_000,"query_id",trino.getQueryId(),
                    "stats",Map.of("elapsed_ms",stats.getElapsedTimeMillis(),"cpu_ms",stats.getCpuTimeMillis(),"processed_rows",stats.getProcessedRows(),"processed_bytes",stats.getProcessedBytes(),"state",stats.getState()));
            }
        }catch(SQLException exception){throw new SqlException(exception.getSQLState()!=null && exception.getSQLState().startsWith("08"));}
    }
    public static String validate(String input) {
        String sql=input.strip();if(sql.endsWith(";"))sql=sql.substring(0,sql.length()-1).strip();
        if(!sql.matches("(?is)^(SELECT|WITH|SHOW|DESCRIBE)\\b.*") || sql.contains(";") || sql.contains("--") || sql.contains("/*"))throw new SqlException(false);
        return sql;
    }
}
