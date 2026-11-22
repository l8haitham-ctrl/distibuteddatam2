package com.atlas.explorer;

import com.atlas.explorer.features.connection.api.ConnectionController;
import com.atlas.explorer.shared.api.ApiExceptionHandler;
import com.atlas.explorer.features.connection.connector.*;
import com.atlas.explorer.shared.domain.ExplorerException;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connection.service.ConnectionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiTests {
    @Test void routesDelegateToServicesAndValidationDoesNotEchoSecrets() throws Exception {
        var service = mock(ConnectionService.class);
        when(service.test(any())).thenReturn(Map.of("status", "connected"));
        var mvc = MockMvcBuilders.standaloneSetup(new ConnectionController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(post("/connections/test").contentType(MediaType.APPLICATION_JSON).content("""
            {"engine":"sqlserver","host":"server","port":1433,"database":"demo",
            "username":"reader","password":"test-secret","trust_server_certificate":false}
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("connected"));
        var result = mvc.perform(post("/connections/test").contentType(MediaType.APPLICATION_JSON).content("""
            {"engine":"invalid-engine!","host":"server","port":1433,"database":"demo","username":"reader","password":"never-echo-me"}
            """)).andExpect(status().isUnprocessableEntity()).andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("never-echo-me"));
        verify(service, times(1)).test(any());
    }
    @Test void domainErrorsAreTranslatedWithoutDriverDetails() throws Exception {
        var service = mock(ConnectionService.class);
        when(service.test(any())).thenThrow(new ExplorerException(ExplorerException.Code.AUTHENTICATION));
        var mvc = MockMvcBuilders.standaloneSetup(new ConnectionController(service)).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(post("/connections/test").contentType(MediaType.APPLICATION_JSON).content("""
            {"engine":"sqlserver","host":"server","port":1433,"database":"demo","username":"reader","password":"secret"}
            """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
    }
    @Test void servicesCanUseAnotherConnectorWithoutAnyHttpDependency() {
        var adapter = mock(DatabaseConnector.class);
        when(adapter.engine()).thenReturn("sqlserver");
        when(adapter.test(any())).thenReturn(Map.of("database_name", "demo"));
        var service = new ConnectionService(new ConnectorRegistry(List.of(adapter)));
        var config = new ConnectionConfig("sqlserver", "server", 1433, "demo", "reader", "secret", false);
        assertEquals("demo", service.test(config).get("database_name"));
        verify(adapter).test(config);
        assertFalse(config.toString().contains("secret"));
    }
    @Test void sqlErrorsAreClassifiedWithoutRetainingTheirMessage() {
        var error = SqlServerConnections.sanitized(new SQLException("private-password", "28000", 18456));
        assertEquals(ExplorerException.Code.AUTHENTICATION, error.code());
        assertFalse(error.getMessage().contains("private-password"));
        assertNull(error.getCause());
        assertEquals(ExplorerException.Code.UNAVAILABLE, SqlServerConnections.sanitized(new SQLException("private", "08001")).code());
    }
}
