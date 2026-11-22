package com.atlas.explorer.features.connection.connector;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connectors.dto.JdbcSettings;
import com.atlas.explorer.shared.domain.ExplorerException;
import java.sql.SQLException;
public final class JdbcConnections {
    private JdbcConnections(){}
    public static JdbcSettings settings(ConnectionConfig c){return new JdbcSettings(c.host(),c.port(),c.database(),c.username(),c.password(),Boolean.TRUE.equals(c.trustServerCertificate()));}
    public static ExplorerException sanitized(SQLException e){
        String state=e.getSQLState()==null?"":e.getSQLState();
        return new ExplorerException(state.startsWith("28")||e.getErrorCode()==18456?ExplorerException.Code.AUTHENTICATION:
            state.startsWith("08")||state.startsWith("HYT")?ExplorerException.Code.UNAVAILABLE:ExplorerException.Code.QUERY);
    }
}
