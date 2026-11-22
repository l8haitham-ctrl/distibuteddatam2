package com.atlas.explorer.features.connection.connector;
import com.atlas.explorer.features.connectors.connector.RelationalPlugin;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import java.sql.SQLException;
import java.util.*;
public final class PluginConnectionConnector implements DatabaseConnector {
    private final RelationalPlugin plugin;
    public PluginConnectionConnector(RelationalPlugin plugin){this.plugin=plugin;}
    public String engine(){return plugin.engine();}
    public Map<String,Object> test(ConnectionConfig config){
        try(var connection=plugin.open(JdbcConnections.settings(config))){
            if(!connection.isValid(5))throw new SQLException("Connection unavailable.","08001");
            return Map.of("database_name",config.database(),"database_user",connection.getMetaData().getUserName());
        }catch(SQLException e){throw JdbcConnections.sanitized(e);}
    }
}
