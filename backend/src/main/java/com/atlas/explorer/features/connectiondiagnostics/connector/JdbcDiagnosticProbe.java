package com.atlas.explorer.features.connectiondiagnostics.connector;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connection.connector.JdbcConnections;
import com.atlas.explorer.features.connectors.connector.PluginRegistry;
import org.springframework.stereotype.Component;
import java.net.*;
import java.sql.*;

@Component
public class JdbcDiagnosticProbe implements DiagnosticProbe {
    private final PluginRegistry plugins;
    public JdbcDiagnosticProbe(PluginRegistry plugins){this.plugins=plugins;}
    public InetAddress[] resolve(String host)throws UnknownHostException{return InetAddress.getAllByName(host);}
    public void reach(InetAddress[] addresses,int port)throws Exception {
        var deadline=System.nanoTime()+5_000_000_000L;
        for(var address:addresses){
            int remaining=(int)((deadline-System.nanoTime())/1_000_000L);if(remaining<=0)break;
            try(var socket=new Socket()){socket.connect(new InetSocketAddress(address,port),Math.min(3000,remaining));return;}
            catch(java.io.IOException ignored){/* Try the next resolved address within the same budget. */}
        }
        throw new SocketTimeoutException("Port unavailable.");
    }
    public Connection open(ConnectionConfig config)throws SQLException{return plugins.get(config.engine()).open(JdbcConnections.settings(config));}
    public void verify(Connection connection)throws SQLException{if(!connection.isValid(5))throw new SQLException("Invalid session.");}
}
