package com.atlas.explorer;

import com.atlas.explorer.features.exploration.connector.*;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SchemaServiceTests {
    @Test void explorationUsesItsOwnReplaceableConnector() {
        var connector=mock(SchemaConnector.class);when(connector.engine()).thenReturn("sqlserver");
        when(connector.explore(any())).thenReturn(List.of(Map.of("table_name","Orders")));
        var service=new ExplorationService(new SchemaConnectorRegistry(List.of(connector)));
        var config=new ConnectionConfig("sqlserver","server",1433,"demo","reader","test",false);
        assertEquals("demo",service.explore(config).get("database"));verify(connector).explore(config);
    }
}
