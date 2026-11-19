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

@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatabaseIntegrationTests {
    @LocalServerPort int port;
    @Autowired SourceService sources;
    private HttpResponse<String> call(String method, String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
    @Test void configuredSourceConnectsAndListsTablesWithoutPassword() throws Exception {
        assertFalse(call("GET", "/sources").body().contains("password"));
        var result = call("POST", "/sources/sqlserver/test");
        assertEquals(200, result.statusCode(), result.body());
        assertEquals("lake_reader", JsonPath.read(result.body(), "$.database_user"));
        List<String> names = JsonPath.read(call("GET", "/sources/sqlserver/tables").body(), "$.tables[*].table_name");
        assertEquals(Set.of("Customer", "Invoice", "Payment"), new HashSet<>(names));
    }
    @Test void metadataRetainsPrecisionKeysAndRelationships() throws Exception {
        var result = call("GET", "/sources/sqlserver/tables/Invoice");
        assertEquals(200, result.statusCode(), result.body());
        assertEquals("invoice_id", JsonPath.read(result.body(), "$.primary_key[0].column_name"));
        assertEquals("Customer", JsonPath.read(result.body(), "$.foreign_keys[0].referenced_table"));
        List<Integer> precisions = JsonPath.read(result.body(), "$.columns[?(@.name == 'amount')].precision");
        assertEquals(List.of(12), precisions);
        List<String> names=JsonPath.read(result.body(),"$.columns[*].name");
        assertEquals(List.of("invoice_id","customer_id","invoice_date","amount","status","updated_at"),names);
        for(String table:List.of("Customer","Invoice","Payment")) {
            var schema=call("GET","/sources/sqlserver/tables/"+table).body();
            List<String> types=JsonPath.read(schema,"$.columns[?(@.name == 'updated_at')].data_type");
            List<Boolean> nullable=JsonPath.read(schema,"$.columns[?(@.name == 'updated_at')].nullable");
            assertEquals(List.of("datetime2"),types);assertEquals(List.of(false),nullable);
        }
    }
    @Test void submittedConnectionsExploreWithoutReturningCredentials() throws Exception {
        var config=sources.get("sqlserver");
        var body=new LinkedHashMap<String,Object>();
        body.put("engine",config.engine());body.put("host",config.host());body.put("port",config.port());
        body.put("database",config.database());body.put("username",config.username());
        body.put("password",config.password());body.put("trust_server_certificate",true);
        var json=com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().toJson(body);
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/connections/explore"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
        var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());
        assertEquals(3,((List<?>)JsonPath.read(response.body(),"$.tables")).size());
        assertFalse(response.body().contains(config.password()));
    }
    @Test void unknownAndInjectedIdentifiersAreNotExecuted() throws Exception {
        assertEquals(404, call("GET", "/sources/missing/tables").statusCode());
        assertEquals(404, call("GET", "/sources/sqlserver/tables/Missing").statusCode());
        var injected = URLEncoder.encode("dbo'; DROP TABLE Invoice;--", java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(404, call("GET", "/sources/sqlserver/tables/Invoice?schema=" + injected).statusCode());
        assertEquals(200, call("GET", "/sources/sqlserver/tables/Invoice").statusCode());
    }
    @Test void readerCannotWriteOrAdministerAndDataIsPreserved() throws Exception {
        try (var connection = SqlServerConnections.open(sources.get("sqlserver"))) {
            var row = SqlServerConnections.query(connection, """
                SELECT HAS_PERMS_BY_NAME('dbo.Invoice', 'OBJECT', 'SELECT') AS can_read,
                    HAS_PERMS_BY_NAME('dbo.Invoice', 'OBJECT', 'INSERT') AS can_insert,
                    HAS_PERMS_BY_NAME('dbo.Invoice', 'OBJECT', 'UPDATE') AS can_update,
                    HAS_PERMS_BY_NAME('dbo.Invoice', 'OBJECT', 'DELETE') AS can_delete,
                    IS_SRVROLEMEMBER('sysadmin') AS is_admin,
                    (SELECT COUNT(*) FROM dbo.Invoice) AS invoices
                """).getFirst();
            assertEquals(1, row.get("can_read"));
            for (String key : List.of("can_insert", "can_update", "can_delete", "is_admin")) assertEquals(0, row.get(key));
            assertEquals(5000, row.get("invoices"));
        }
    }
}
