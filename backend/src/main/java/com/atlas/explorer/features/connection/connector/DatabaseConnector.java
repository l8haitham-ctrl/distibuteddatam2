package com.atlas.explorer.features.connection.connector;

import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import java.util.List;
import java.util.Map;

public interface DatabaseConnector {
    String engine();
    Map<String, Object> test(ConnectionConfig config);
}
