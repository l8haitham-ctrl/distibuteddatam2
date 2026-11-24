package com.atlas.explorer.features.glossary;

import com.atlas.explorer.features.glossary.connector.*;
import com.atlas.explorer.features.glossary.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import com.atlas.explorer.features.jobs.config.JobProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.net.*;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_GLOSSARY_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlossaryIntegrationTests {
    @LocalServerPort int port;
    @Autowired DataSource dataSource;
    @Autowired JobProperties properties;
    private final ObjectMapper json=new ObjectMapper();
    private final List<UUID> created=new ArrayList<>();
    private HttpResponse<String> call(String method,String path,String body)throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if(body==null)builder.method(method,HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body));
        return com.atlas.explorer.support.AuthenticatedHttp.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    @BeforeEach void isolate() { assertTrue(properties.jdbcUrl().endsWith("/atlas_test")); }
    @AfterEach void cleanup() {
        var jdbc=new JdbcTemplate(dataSource);
        for(var id:created)jdbc.update("DELETE FROM business_term WHERE id=?",id);
    }
    private UUID create(String name)throws Exception {
        var body=json.writeValueAsString(Map.of("name",name,"definition","Montant restant du","calculation_rule","Facture - paiements","owner","Finance","status","DRAFT"));
        var response=call("POST","/glossary/terms",body);assertEquals(201,response.statusCode(),response.body());
        var id=UUID.fromString(json.readTree(response.body()).get("id").asText());created.add(id);
        assertEquals("/glossary/terms/"+id,response.headers().firstValue("Location").orElseThrow());return id;
    }
    @Test void persistsEditsSearchAndLinksRealSqlServerMetadata()throws Exception {
        String name="Glossary-test-"+UUID.randomUUID();var id=create(name);
        String target="{\"source_id\":\"sqlserver\",\"schema_name\":\"dbo\",\"table_name\":\"Invoice\",\"column_name\":\"amount\"}";
        var linked=call("POST","/glossary/terms/"+id+"/links",target);assertEquals(201,linked.statusCode(),linked.body());
        var linkId=json.readTree(linked.body()).get("id").asText();
        assertEquals(409,call("POST","/glossary/terms/"+id+"/links",target).statusCode());
        // A new repository instance reads persisted PostgreSQL state, rather than an in-memory cache.
        var stored=new JdbcGlossaryRepository(new JdbcTemplate(dataSource)).get(id);
        assertEquals(1,stored.links().size());assertEquals("amount",stored.links().getFirst().columnName());
        var update=json.writeValueAsString(Map.of("name",name,"definition","Definition validee","status","VALIDATED"));
        assertEquals(200,call("PUT","/glossary/terms/"+id,update).statusCode());
        var reloaded=new JdbcGlossaryRepository(new JdbcTemplate(dataSource)).get(id);
        assertEquals(TermRequest.Status.VALIDATED,reloaded.status());assertEquals(1,reloaded.links().size());
        var results=json.readTree(call("GET","/glossary/terms?q="+name+"&limit=1&offset=0",null).body());
        assertEquals(1,results.size());assertEquals(id.toString(),results.get(0).get("id").asText());
        assertEquals(0,json.readTree(call("GET","/glossary/terms?q="+name+"&offset=1",null).body()).size());
        var reverse=call("GET","/glossary/links?source_id=sqlserver&schema_name=dbo&table_name=Invoice&column_name=amount",null);
        assertEquals(200,reverse.statusCode(),reverse.body());assertTrue(reverse.body().contains(id.toString()));
        var other=create(name+"-other");
        assertEquals(404,call("DELETE","/glossary/terms/"+other+"/links/"+linkId,null).statusCode());
        assertEquals(204,call("DELETE","/glossary/terms/"+id+"/links/"+linkId,null).statusCode());
        assertTrue(new JdbcGlossaryRepository(new JdbcTemplate(dataSource)).get(id).links().isEmpty());
    }
    @Test void rejectsDuplicatesInvalidTargetsAndInvalidQueries()throws Exception {
        String name="Glossary-test-"+UUID.randomUUID();var id=create(name);
        assertEquals(409,call("POST","/glossary/terms",json.writeValueAsString(Map.of("name"," "+name.toUpperCase(Locale.ROOT)+" ","definition","d","status","DRAFT"))).statusCode());
        assertEquals(422,call("POST","/glossary/terms/"+id+"/links","{\"source_id\":\"sqlserver\",\"schema_name\":\"dbo\",\"table_name\":\"Invoice\",\"column_name\":\"missing\"}").statusCode());
        assertEquals(422,call("POST","/glossary/terms/"+id+"/links","{\"source_id\":\"sqlserver\",\"schema_name\":\"dbo\",\"table_name\":\"MissingTable\"}").statusCode());
        assertEquals(404,call("POST","/glossary/terms/"+id+"/links","{\"source_id\":\"unknown\",\"schema_name\":\"dbo\",\"table_name\":\"Invoice\"}").statusCode());
        assertEquals(422,call("GET","/glossary/terms?limit=101",null).statusCode());
        assertEquals(422,call("GET","/glossary/terms?offset=-1",null).statusCode());
        assertEquals(422,call("GET","/glossary/terms/not-a-uuid",null).statusCode());
        assertEquals(422,call("GET","/glossary/links?source_id=&schema_name=dbo&table_name=Invoice",null).statusCode());
        assertEquals(0,json.readTree(call("GET","/glossary/terms/"+id,null).body()).get("links").size());
    }
    @Test void tableLinksAreDistinctAndReverseLookupKeepsSourcesSeparate()throws Exception {
        var id=create("Glossary-test-"+UUID.randomUUID());
        String table="{\"source_id\":\"sqlserver\",\"schema_name\":\"dbo\",\"table_name\":\"Invoice\"}";
        assertEquals(201,call("POST","/glossary/terms/"+id+"/links",table).statusCode());
        assertEquals(409,call("POST","/glossary/terms/"+id+"/links",table).statusCode());
        assertEquals(0,json.readTree(call("GET","/glossary/links?source_id=other&schema_name=dbo&table_name=Invoice",null).body()).size());
        assertEquals(0,json.readTree(call("GET","/glossary/links?source_id=sqlserver&schema_name=dbo&table_name=Invoice&column_name=amount",null).body()).size());
    }
    @Test @EnabledIfEnvironmentVariable(named="RUN_PG_TESTS",matches="true")
    void oneTermCanReferenceSqlServerAndPostgresqlIndependently()throws Exception {
        var id=create("Glossary-test-"+UUID.randomUUID());
        for(var target:List.of(
                Map.of("source_id","sqlserver","schema_name","dbo","table_name","Invoice","column_name","amount"),
                Map.of("source_id","postgresql","schema_name","public","table_name","invoice","column_name","amount"))) {
            var response=call("POST","/glossary/terms/"+id+"/links",json.writeValueAsString(target));
            assertEquals(201,response.statusCode(),response.body());
        }
        var stored=new JdbcGlossaryRepository(new JdbcTemplate(dataSource)).get(id);
        assertEquals(Set.of("sqlserver","postgresql"),new HashSet<>(stored.links().stream().map(TermLink::sourceId).toList()));
    }
}
