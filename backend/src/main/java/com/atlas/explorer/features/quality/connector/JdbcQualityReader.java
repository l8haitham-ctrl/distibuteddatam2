package com.atlas.explorer.features.quality.connector;
import org.springframework.stereotype.Component;
import java.sql.*;
import java.util.*;
@Component
public class JdbcQualityReader implements QualityReader {
    public Map<String,Map<String,String>> read(Connection connection,Map<String,String> queries) throws SQLException {
        var results=new LinkedHashMap<String,Map<String,String>>();
        for(var query:queries.entrySet())try(var statement=connection.createStatement()){
            statement.setQueryTimeout(30);statement.setMaxRows(2);
            try(var rows=statement.executeQuery(query.getValue())){
                if(!rows.next())throw new SQLException("Missing aggregate");
                var values=new LinkedHashMap<String,String>();var metadata=rows.getMetaData();
                for(int i=1;i<=metadata.getColumnCount();i++)values.put(metadata.getColumnLabel(i).toLowerCase(Locale.ROOT),rows.getString(i));
                if(rows.next())throw new SQLException("Unexpected aggregate rows");results.put(query.getKey(),values);
            }
        }
        return results;
    }
}
