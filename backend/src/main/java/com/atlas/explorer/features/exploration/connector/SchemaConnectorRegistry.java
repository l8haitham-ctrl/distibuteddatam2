package com.atlas.explorer.features.exploration.connector;

import com.atlas.explorer.shared.domain.ExplorerException;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SchemaConnectorRegistry {
    private final List<SchemaConnector> connectors;
    public SchemaConnectorRegistry(List<SchemaConnector> connectors) { this.connectors = List.copyOf(connectors); }
    public SchemaConnector get(String engine) {
        return connectors.stream().filter(c -> c.engine().equals(engine)).findFirst()
                .orElseThrow(() -> new ExplorerException(ExplorerException.Code.SOURCE_NOT_FOUND));
    }
}
