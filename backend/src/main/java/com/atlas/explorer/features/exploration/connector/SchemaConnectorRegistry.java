package com.atlas.explorer.features.exploration.connector;

import com.atlas.explorer.shared.domain.ExplorerException;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SchemaConnectorRegistry {
    private final List<SchemaConnector> connectors;
    private final com.atlas.explorer.features.connectors.connector.PluginRegistry plugins;
    public SchemaConnectorRegistry(List<SchemaConnector> connectors){this(connectors,com.atlas.explorer.features.connectors.connector.PluginRegistry.discover());}
    @org.springframework.beans.factory.annotation.Autowired
    public SchemaConnectorRegistry(List<SchemaConnector> connectors,com.atlas.explorer.features.connectors.connector.PluginRegistry plugins) { this.connectors = List.copyOf(connectors);this.plugins=plugins; }
    public SchemaConnector get(String engine) {
        return connectors.stream().filter(c -> c.engine().equals(engine)).findFirst()
                .orElseGet(()->{try{return new JdbcSchemaConnector(plugins.get(engine));}catch(IllegalArgumentException e){throw new ExplorerException(ExplorerException.Code.CONNECTOR_NOT_FOUND);}});
    }
}
