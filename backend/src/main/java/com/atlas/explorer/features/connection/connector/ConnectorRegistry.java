package com.atlas.explorer.features.connection.connector;

import com.atlas.explorer.shared.domain.ExplorerException;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ConnectorRegistry {
    private final List<DatabaseConnector> connectors;
    public ConnectorRegistry(List<DatabaseConnector> connectors) { this.connectors = List.copyOf(connectors); }
    public DatabaseConnector get(String engine) {
        return connectors.stream().filter(c -> c.engine().equals(engine)).findFirst()
                .orElseThrow(() -> new ExplorerException(ExplorerException.Code.SOURCE_NOT_FOUND));
    }
}
