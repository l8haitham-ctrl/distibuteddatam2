package com.atlas.explorer.features.connectiondiagnostics;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connectiondiagnostics.connector.DiagnosticProbe;
import com.atlas.explorer.features.connectiondiagnostics.service.ConnectionDiagnostics;
import com.atlas.explorer.features.connectiondiagnostics.dto.DiagnosticEvent;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiagnosticsTests {
    private final ConnectionConfig config=new ConnectionConfig("sqlserver","sqlserver",1433,"BusinessDB","reader","private-password",true);
    @Test void streamsStagesAroundRealOperationsAndClosesConnection()throws Exception {
        var probe=mock(DiagnosticProbe.class);var connection=mock(Connection.class);var events=new ArrayList<DiagnosticEvent>();
        when(probe.resolve("sqlserver")).thenReturn(new InetAddress[]{InetAddress.getLoopbackAddress()});when(probe.open(config)).thenReturn(connection);
        new ConnectionDiagnostics(probe).run(config,events::add,()->false);
        assertEquals(List.of("dns:running","dns:passed","tcp:running","tcp:passed","jdbc:running","jdbc:passed","validation:running","validation:passed","complete:passed"),events.stream().map(e->e.step()+":"+e.state()).toList());
        verify(connection).close();verify(probe).verify(connection);assertTrue(events.stream().allMatch(e->e.durationMs()>=0));
    }
    @Test void dnsFailureSkipsLaterChecksAndDoesNotLeakDriverDetails()throws Exception {
        var probe=mock(DiagnosticProbe.class);when(probe.resolve(any())).thenThrow(new UnknownHostException("password=private-password"));var events=new ArrayList<DiagnosticEvent>();
        new ConnectionDiagnostics(probe).run(config,events::add,()->false);
        assertEquals("failed",events.get(1).state());assertEquals(3,events.stream().filter(e->e.state().equals("skipped")).count());verify(probe,never()).open(any());
        assertFalse(events.toString().contains("private-password"));assertEquals("failed",events.getLast().state());
    }
    @Test void jdbcAuthenticationFailureIsReportedAtCombinedJdbcStep()throws Exception {
        var probe=mock(DiagnosticProbe.class);when(probe.resolve(any())).thenReturn(new InetAddress[]{InetAddress.getLoopbackAddress()});when(probe.open(config)).thenThrow(new SQLException("secret jdbc url","28000",18456));var events=new ArrayList<DiagnosticEvent>();
        new ConnectionDiagnostics(probe).run(config,events::add,()->false);
        var failed=events.stream().filter(e->e.step().equals("jdbc")&&e.state().equals("failed")).findFirst().orElseThrow();assertTrue(failed.message().contains("Authentification"));assertFalse(events.toString().contains("secret jdbc"));verify(probe,never()).verify(any());
    }
    @Test void cancelledTransportDoesNotInventASuccess()throws Exception {
        var probe=mock(DiagnosticProbe.class);var events=new ArrayList<DiagnosticEvent>();new ConnectionDiagnostics(probe).run(config,events::add,()->true);assertTrue(events.isEmpty());verifyNoInteractions(probe);
    }
}
