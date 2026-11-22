package com.atlas.explorer.features.connectors.connector;

import com.atlas.explorer.features.connectors.dto.JdbcSettings;
import java.util.Properties;

public final class SqlServerPlugin implements RelationalPlugin {
    public String engine(){return "sqlserver";}
    public String displayName(){return "Microsoft SQL Server";}
    public String driverClass(){return "com.microsoft.sqlserver.jdbc.SQLServerDriver";}
    public int defaultPort(){return 1433;}
    public String defaultSchema(){return "dbo";}
    public String jdbcUrl(JdbcSettings settings){return "jdbc:sqlserver://"+settings.authority();}
    public Properties properties(JdbcSettings settings){
        var p=RelationalPlugin.super.properties(settings);
        p.setProperty("databaseName",settings.database());p.setProperty("encrypt","true");
        p.setProperty("trustServerCertificate",Boolean.toString(settings.trustCertificate()));
        p.setProperty("loginTimeout","5");p.setProperty("queryTimeout","10");p.setProperty("socketTimeout","15000");return p;
    }
    public String quote(String value){return "["+value.replace("]","]]")+"]";}
    public String distinctSql(String schema,String table,String column){return "SELECT COUNT_BIG(DISTINCT "+quote(column)+") AS cardinality FROM "+qualified(schema,table);}
}
