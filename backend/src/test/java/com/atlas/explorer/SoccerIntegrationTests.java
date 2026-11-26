package com.atlas.explorer;

import com.atlas.explorer.features.connection.connector.SqlServerConnections;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.support.AuthenticatedHttp;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_SOCCER_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class SoccerIntegrationTests {
    @LocalServerPort int port;
    @Autowired SourceService sources;
    private String call(String method,String path)throws Exception {
        var response=AuthenticatedHttp.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
            .method(method,HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());
        assertFalse(response.body().contains(sources.get("soccer").password()));
        return response.body();
    }
    @Test void sourceConnectsAndExposesSevenTables()throws Exception {
        assertEquals("lake_reader",JsonPath.read(call("POST","/sources/soccer/test"),"$.database_user"));
        List<String> names=JsonPath.read(call("GET","/sources/soccer/tables"),"$.tables[*].table_name");
        assertEquals(Set.of("Country","League","Team","Player","Match","Team_Attributes","Player_Attributes"),new HashSet<>(names));
    }
    @Test void schemaExposesDatePrimaryKeyAndRelations()throws Exception {
        var schema=call("GET","/sources/soccer/tables/Match");
        assertEquals("id",JsonPath.read(schema,"$.primary_key[0].column_name"));
        assertEquals(List.of("datetime2"),JsonPath.read(schema,"$.columns[?(@.name == 'date')].data_type"));
        List<String> parents=JsonPath.read(schema,"$.foreign_keys[*].referenced_table");
        assertEquals(Set.of("Country","League","Team"),new HashSet<>(parents));
    }
    @Test void importedRowsAreReadableWithoutWritePrivileges()throws Exception {
        try(var connection=SqlServerConnections.open(sources.get("soccer"))) {
            var row=SqlServerConnections.query(connection,"""
                SELECT (SELECT COUNT(*) FROM dbo.[Match]) AS matches,
                    (SELECT COUNT(*) FROM dbo.Player) AS players,
                    (SELECT COUNT(*) FROM dbo.Player_Attributes) AS attributes,
                    HAS_PERMS_BY_NAME('dbo.Match','OBJECT','INSERT') AS can_insert,
                    HAS_PERMS_BY_NAME('dbo.Match','OBJECT','UPDATE') AS can_update,
                    HAS_PERMS_BY_NAME('dbo.Match','OBJECT','DELETE') AS can_delete,
                    IS_SRVROLEMEMBER('sysadmin') AS is_admin
                """).getFirst();
            assertEquals(25979,row.get("matches")); assertEquals(11060,row.get("players"));
            assertEquals(183978,row.get("attributes"));
            for(var key:List.of("can_insert","can_update","can_delete","is_admin"))assertEquals(0,row.get(key));
        }
    }
}
