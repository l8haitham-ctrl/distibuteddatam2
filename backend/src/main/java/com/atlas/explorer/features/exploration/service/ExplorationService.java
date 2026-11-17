package com.atlas.explorer.features.exploration.service;

import com.atlas.explorer.features.exploration.connector.SchemaConnectorRegistry;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ExplorationService {
    private final SchemaConnectorRegistry registry;
    public ExplorationService(SchemaConnectorRegistry registry) { this.registry = registry; }
    public Map<String, Object> tables(ConnectionConfig config) { return Map.of("tables", registry.get(config.engine()).tables(config)); }
    public Map<String, Object> describe(ConnectionConfig config, String schema, String table) { return registry.get(config.engine()).describe(config, schema, table); }
    public Map<String, Object> explore(ConnectionConfig config) { return Map.of("database", config.database(), "tables", registry.get(config.engine()).explore(config)); }
}
