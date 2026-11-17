package com.atlas.explorer.features.connection.service;

import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.shared.domain.ExplorerException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SourceService {
    private final Environment environment;
    public SourceService(Environment environment) { this.environment = environment; }
    public ConnectionConfig get(String id) {
        if (!"sqlserver".equals(id)) throw new ExplorerException(ExplorerException.Code.SOURCE_NOT_FOUND);
        return new ConnectionConfig("sqlserver", environment.getProperty("source.host", "sqlserver"),
                environment.getProperty("source.port", Integer.class, 1433), environment.getProperty("source.database", "BusinessDB"),
                environment.getProperty("source.username", "lake_reader"), environment.getProperty("source.password", ""),
                environment.getProperty("source.trust-server-certificate", Boolean.class, false));
    }
    public List<Map<String, Object>> list() {
        var c = get("sqlserver");
        return List.of(Map.of("id", "sqlserver", "engine", c.engine(), "host", c.host(), "port", c.port(), "database", c.database(), "username", c.username()));
    }
}
