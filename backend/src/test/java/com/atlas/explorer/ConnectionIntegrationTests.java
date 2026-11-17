package com.atlas.explorer;

import com.atlas.explorer.features.connection.connector.SqlServerConnections;
import com.atlas.explorer.features.connection.service.SourceService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.*;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConnectionIntegrationTests {
    @LocalServerPort int port;
    @Autowired SourceService sources;
    private HttpResponse<String> call(String method,String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
                .method(method,HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void connectsWithReaderAndHidesCredentials() throws Exception {
        assertFalse(call("GET","/sources").body().contains("password"));
        var response=call("POST","/sources/sqlserver/test");
        assertEquals(200,response.statusCode(),response.body());
        assertEquals("lake_reader",JsonPath.read(response.body(),"$.database_user"));
        assertEquals(404,call("POST","/sources/unknown/test").statusCode());
    }
    @Test void readerHasNoWriteOrAdministratorPrivileges() throws Exception {
        try(var connection=SqlServerConnections.open(sources.get("sqlserver"))) {
            var row=SqlServerConnections.query(connection,"""
                SELECT HAS_PERMS_BY_NAME('dbo.Invoice','OBJECT','SELECT') AS can_read,
                HAS_PERMS_BY_NAME('dbo.Invoice','OBJECT','INSERT') AS can_insert,
                HAS_PERMS_BY_NAME('dbo.Invoice','OBJECT','UPDATE') AS can_update,
                HAS_PERMS_BY_NAME('dbo.Invoice','OBJECT','DELETE') AS can_delete,
                IS_SRVROLEMEMBER('sysadmin') AS is_admin
                """).getFirst();
            assertEquals(1,row.get("can_read"));
            for(var key:List.of("can_insert","can_update","can_delete","is_admin"))assertEquals(0,row.get(key));
        }
    }
}
