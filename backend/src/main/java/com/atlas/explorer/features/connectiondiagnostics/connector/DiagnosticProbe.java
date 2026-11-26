package com.atlas.explorer.features.connectiondiagnostics.connector;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import java.net.InetAddress;
import java.sql.Connection;
public interface DiagnosticProbe {
    InetAddress[] resolve(String host)throws Exception;
    void reach(InetAddress[] addresses,int port)throws Exception;
    Connection open(ConnectionConfig config)throws Exception;
    void verify(Connection connection)throws Exception;
}
