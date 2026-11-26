package com.atlas.explorer.features.nativesources;

import com.atlas.explorer.features.nativesources.connector.ClickhouseNativeConnector;
import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import com.atlas.explorer.features.nativesources.service.*;
import com.atlas.explorer.support.AuthenticatedHttp;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_CLICKHOUSE_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClickhouseIntegrationTests {
    @LocalServerPort int port;
    NativeConnection config(String password,String namespace,String prefix){return new NativeConnection("clickhouse","clickhouse-source",8123,namespace,"atlas_reader",password,false,prefix);}
    String password(){return System.getenv("CLICKHOUSE_READER_PASSWORD");}
    String call(String method,String path)throws Exception {
        var response=AuthenticatedHttp.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
            .method(method,HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());assertFalse(response.body().contains(password()));return response.body();
    }
    @Test void configuredApiConnectsAndExposesOlapTypesAndPartitionMetadata()throws Exception {
        assertTrue(call("GET","/native/connectors").contains("clickhouse"));
        assertTrue(call("GET","/native/sources").contains("clickhouse-demo"));
        assertEquals("connected",JsonPath.read(call("POST","/native/sources/clickhouse-demo/test"),"$.status"));
        var result=call("POST","/native/sources/clickhouse-demo/explore");
        assertEquals(List.of("customer","invoice","payment"),JsonPath.read(result,"$.objects[*].name"));
        assertEquals(List.of("MergeTree","MergeTree","MergeTree"),JsonPath.read(result,"$.objects[*].type"));
        assertTrue(result.contains("Nullable(String)"));assertTrue(result.contains("Decimal(12, 2)"));
        assertTrue(result.contains("\"partition_key\":true"));assertFalse(result.contains("Client1"));
    }
    @Test void prefixesAreLiteralAndInvalidDatabaseOrCredentialsAreSanitized(){
        var service=new NativeSourceService(List.of(new ClickhouseNativeConnector()));
        var result=service.explore(config(password(),"analytics","inv"));
        assertEquals(1,((List<?>)result.get("objects")).size());
        assertEquals(0,((List<?>)service.explore(config(password(),"analytics","inv%_")).get("objects")).size());
        for(var config:List.of(config("wrong-secret-value","analytics",""),config(password(),"analytics'; DROP DATABASE analytics;--",""))){
            var error=assertThrows(NativeSourceException.class,()->service.test(config));
            assertFalse(error.getMessage().contains("wrong-secret"));assertFalse(error.getMessage().contains(password()));
        }
    }
    @Test void readerCannotWriteAndDemoCountsRemainIntact()throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://clickhouse-source:8123/"))
            .header("X-ClickHouse-User","atlas_reader").header("X-ClickHouse-Key",password())
            .POST(HttpRequest.BodyPublishers.ofString("INSERT INTO analytics.customer SELECT * FROM analytics.customer WHERE 0")).build();
        var client=HttpClient.newHttpClient();assertNotEquals(200,client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode());
        var count=HttpRequest.newBuilder(URI.create("http://clickhouse-source:8123/"))
            .header("X-ClickHouse-User","atlas_reader").header("X-ClickHouse-Key",password())
            .POST(HttpRequest.BodyPublishers.ofString("SELECT (SELECT count() FROM analytics.customer),(SELECT count() FROM analytics.invoice),(SELECT count() FROM analytics.payment) FORMAT TabSeparated")).build();
        var response=client.send(count,HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode());assertEquals("1000\t5000\t8000",response.body().trim());
    }
}
