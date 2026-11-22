package com.atlas.explorer.features.connectors.connector;
import com.atlas.explorer.features.connectors.dto.JdbcSettings;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PostgresqlPlugin implements RelationalPlugin {
    public String engine(){return "postgresql";}
    public String displayName(){return "PostgreSQL";}
    public String driverClass(){return "org.postgresql.Driver";}
    public int defaultPort(){return 5432;}
    public String defaultSchema(){return "public";}
    public String jdbcUrl(JdbcSettings settings){return "jdbc:postgresql://"+settings.authority()+"/"+URLEncoder.encode(settings.database(),StandardCharsets.UTF_8).replace("+","%20");}
    public Properties properties(JdbcSettings settings){
        var p=RelationalPlugin.super.properties(settings);p.setProperty("sslmode",settings.trustCertificate()?"require":"verify-full");
        p.setProperty("connectTimeout","5");p.setProperty("socketTimeout","15");
        p.setProperty("readOnly","true");p.setProperty("readOnlyMode","always");p.setProperty("ApplicationName","data-platform");return p;
    }
    public Map<String,String> sparkOptions(JdbcSettings settings){var result=new HashMap<>(RelationalPlugin.super.sparkOptions(settings));result.put("socketTimeout","600");return result;}
}
