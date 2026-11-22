package com.atlas.explorer.features.connection.connector;

import com.atlas.explorer.shared.domain.ExplorerException;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ConnectorRegistry {
    private final List<DatabaseConnector> connectors;
    private final com.atlas.explorer.features.connectors.connector.PluginRegistry plugins;
    public ConnectorRegistry(List<DatabaseConnector> connectors){this(connectors,com.atlas.explorer.features.connectors.connector.PluginRegistry.discover());}
    @org.springframework.beans.factory.annotation.Autowired
    public ConnectorRegistry(List<DatabaseConnector> connectors,com.atlas.explorer.features.connectors.connector.PluginRegistry plugins) { this.connectors = List.copyOf(connectors);this.plugins=plugins; }
    public DatabaseConnector get(String engine) {
        return connectors.stream().filter(c -> c.engine().equals(engine)).findFirst()
                .orElseGet(()->{try{return new PluginConnectionConnector(plugins.get(engine));}catch(IllegalArgumentException e){throw new ExplorerException(ExplorerException.Code.CONNECTOR_NOT_FOUND);}});
    }
}
