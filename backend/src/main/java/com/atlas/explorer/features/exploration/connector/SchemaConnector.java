package com.atlas.explorer.features.exploration.connector;

import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import java.util.*;

public interface SchemaConnector {
    String engine();
    List<Map<String, Object>> tables(ConnectionConfig config);
    Map<String, Object> describe(ConnectionConfig config, String schema, String table);
    List<Map<String, Object>> explore(ConnectionConfig config);
}
