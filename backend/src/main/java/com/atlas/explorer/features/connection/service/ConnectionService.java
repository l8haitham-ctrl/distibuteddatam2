package com.atlas.explorer.features.connection.service;

import com.atlas.explorer.features.connection.connector.ConnectorRegistry;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ConnectionService {
    private final ConnectorRegistry registry;
    public ConnectionService(ConnectorRegistry registry) { this.registry = registry; }
    public Map<String, Object> test(ConnectionConfig config) {
        var result = new LinkedHashMap<>(registry.get(config.engine()).test(config));
        result.put("status", "connected"); return result;
    }
}
