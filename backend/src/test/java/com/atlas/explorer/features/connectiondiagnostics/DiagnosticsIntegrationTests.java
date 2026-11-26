package com.atlas.explorer.features.connectiondiagnostics;
import com.atlas.explorer.support.AuthenticatedHttp;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiagnosticsIntegrationTests {
    @LocalServerPort int port;
    private HttpResponse<String> run(String source)throws Exception {
        return AuthenticatedHttp.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/sources/"+source+"/test/events"))
                .timeout(Duration.ofSeconds(95)).POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());
    }
    private void verify(String source)throws Exception {
        var response=run(source);assertEquals(200,response.statusCode(),response.body());assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/event-stream"));
        assertEquals("no",response.headers().firstValue("X-Accel-Buffering").orElseThrow());var mapper=new ObjectMapper();var stages=new ArrayList<String>();
        for(var line:response.body().lines().filter(l->l.startsWith("data:")).toList()){var event=mapper.readTree(line.substring(5));stages.add(event.get("step").asText()+":"+event.get("state").asText());}
        assertEquals(List.of("dns:running","dns:passed","tcp:running","tcp:passed","jdbc:running","jdbc:passed","validation:running","validation:passed","complete:passed"),stages);
        assertFalse(response.body().contains("password"));
    }
    @Test void realSqlServerStreamsCompleteDiagnosticAndUnknownSourceRemains404()throws Exception {verify("sqlserver");assertEquals(404,run("missing").statusCode());}
    @Test @EnabledIfEnvironmentVariable(named="RUN_PG_TESTS",matches="true") void realPostgresqlUsesTheSameDiagnosticContract()throws Exception {verify("postgresql");}
}
