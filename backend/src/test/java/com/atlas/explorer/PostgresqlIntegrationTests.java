package com.atlas.explorer;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.connection.connector.JdbcConnections;
import com.atlas.explorer.features.connectors.connector.PluginRegistry;
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

@EnabledIfEnvironmentVariable(named="RUN_PG_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostgresqlIntegrationTests {
    @LocalServerPort int port;@Autowired SourceService sources;@Autowired PluginRegistry plugins;
    private HttpResponse<String> call(String method,String path,String body)throws Exception{
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if(body==null)builder.method(method,HttpRequest.BodyPublishers.noBody());else builder.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void sameApisDiscoverBothEnginesAndPostgresqlTables()throws Exception{
        List<String> engines=JsonPath.read(call("GET","/connectors",null).body(),"$[*].engine");assertTrue(engines.containsAll(List.of("sqlserver","postgresql")));
        var list=call("GET","/sources",null);assertFalse(list.body().contains("password"));
        var connected=call("POST","/sources/postgresql/test",null);assertEquals(200,connected.statusCode(),connected.body());assertEquals("atlas_reader",JsonPath.read(connected.body(),"$.database_user"));
        List<String> tables=JsonPath.read(call("GET","/sources/postgresql/tables",null).body(),"$.tables[*].table_name");assertEquals(Set.of("customer","invoice","payment"),new HashSet<>(tables));
        assertEquals(200,call("GET","/sources/sqlserver/tables/Invoice",null).statusCode());
    }
    @Test void jdbcMetadataRetainsPrecisionLengthsAndRelationships()throws Exception{
        var response=call("GET","/sources/postgresql/tables/invoice",null);assertEquals(200,response.statusCode(),response.body());
        String body=response.body();assertEquals("public",JsonPath.read(body,"$.schema_name"));assertEquals("invoice_id",JsonPath.read(body,"$.primary_key[0].column_name"));
        assertEquals("customer",JsonPath.read(body,"$.foreign_keys[0].referenced_table"));assertEquals(List.of(12),JsonPath.read(body,"$.columns[?(@.name == 'amount')].precision"));
        assertEquals(List.of(2),JsonPath.read(body,"$.columns[?(@.name == 'amount')].scale"));assertEquals(List.of(20),JsonPath.read(body,"$.columns[?(@.name == 'status')].character_length"));
        assertEquals(404,call("GET","/sources/postgresql/tables/invoic_",null).statusCode());
        var candidates=call("GET","/storage/jobs/partition-candidates?source_id=postgresql&table=invoice",null);assertEquals(200,candidates.statusCode(),candidates.body());
        List<String> eligible=JsonPath.read(candidates.body(),"$[?(@.eligible == true)].name");assertTrue(eligible.containsAll(List.of("invoice_date","status")));assertFalse(eligible.contains("invoice_id"));
    }
    @Test void sourceUsesTlsAndHasSelectOnlyPrivileges()throws Exception{
        try(var c=plugins.get("postgresql").open(JdbcConnections.settings(sources.get("postgresql")));var statement=c.createStatement()){
            try(var rs=statement.executeQuery("SELECT ssl FROM pg_stat_ssl WHERE pid=pg_backend_pid()")){assertTrue(rs.next());assertTrue(rs.getBoolean(1));}
            try(var rs=statement.executeQuery("SELECT has_table_privilege(current_user,'public.invoice','SELECT'),has_table_privilege(current_user,'public.invoice','INSERT'),has_table_privilege(current_user,'public.invoice','UPDATE'),has_table_privilege(current_user,'public.invoice','DELETE')")){rs.next();assertTrue(rs.getBoolean(1));for(int i=2;i<=4;i++)assertFalse(rs.getBoolean(i));}
            try(var rs=statement.executeQuery("SELECT COUNT(*) FROM public.invoice")){rs.next();assertEquals(5000,rs.getLong(1));}
            assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("UPDATE public.invoice SET amount=amount WHERE false"));
        }
    }
    @Test void wrongCredentialsAreSanitizedWithoutEchoingThePassword()throws Exception{
        var response=call("POST","/connections/test","{\"engine\":\"postgresql\",\"host\":\"postgres-source\",\"port\":5432,\"database\":\"business\",\"username\":\"atlas_reader\",\"password\":\"private-invalid-test-only\",\"trust_server_certificate\":true}");
        assertEquals(400,response.statusCode(),response.body());assertFalse(response.body().contains("private-invalid-test-only"));
    }
}
