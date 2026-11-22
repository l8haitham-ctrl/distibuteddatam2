package com.atlas.explorer.features.connectors.connector;

import com.atlas.explorer.features.connectors.dto.JdbcSettings;
import java.sql.*;
import java.util.*;

/** JDBC SPI. A provider JAR plus its JDBC driver extends both exploration and ingestion. */
public interface RelationalPlugin {
    String engine();
    String displayName();
    String driverClass();
    int defaultPort();
    String defaultSchema();
    default String defaultSchema(JdbcSettings settings){return defaultSchema();}
    String jdbcUrl(JdbcSettings settings);
    default Properties properties(JdbcSettings settings){var p=new Properties();p.setProperty("user",settings.username());p.setProperty("password",settings.password());return p;}
    default Connection open(JdbcSettings settings) throws SQLException {
        try {
            var driver=(Driver)Class.forName(driverClass(),true,getClass().getClassLoader()).getDeclaredConstructor().newInstance();
            var connection=driver.connect(jdbcUrl(settings),properties(settings));
            if(connection==null)throw new SQLException("Driver unavailable.","08001");
            try{connection.setReadOnly(true);return connection;}catch(SQLException e){connection.close();throw e;}
        }catch(ReflectiveOperationException e){throw new SQLException("Driver unavailable.","08001");}
    }
    default String quote(String identifier){return "\""+identifier.replace("\"","\"\"")+"\"";}
    default String qualified(String schema,String table){return quote(schema)+"."+quote(table);}
    default String distinctSql(String schema,String table,String column){return "SELECT COUNT(DISTINCT "+quote(column)+") AS cardinality FROM "+qualified(schema,table);}
    default String metadataCatalog(Connection connection,String schema)throws SQLException{return connection.getCatalog();}
    default String metadataSchema(String schema){return schema;}
    default boolean userSchema(String schema){return schema!=null&&!schema.equalsIgnoreCase("information_schema")&&!schema.startsWith("pg_");}
    default Map<String,String> sparkOptions(JdbcSettings settings){
        var options=new HashMap<String,String>();properties(settings).forEach((k,v)->options.put(k.toString(),v.toString()));
        options.put("url",jdbcUrl(settings));options.put("driver",driverClass());return options;
    }
}
