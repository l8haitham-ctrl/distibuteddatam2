package com.atlas.explorer.features.nativesources;

import com.atlas.explorer.features.nativesources.connector.ClickhouseNativeConnector;
import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import com.atlas.explorer.features.nativesources.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ClickhouseConnectorTests {
    HttpServer server;
    final ObjectMapper json=new ObjectMapper();
    final ClickhouseNativeConnector connector=new ClickhouseNativeConnector();
    @AfterEach void close(){if(server!=null)server.stop(0);}
    NativeConnection config(String namespace,String prefix){return new NativeConnection("clickhouse","127.0.0.1",server.getAddress().getPort(),namespace,"reader","private-test-only",false,prefix);}
    @Test void bindsNamespaceAndLiteralPrefixWithoutEmbeddingThemInSql()throws Exception {
        String namespace="analytics'; DROP DATABASE nope;--",prefix="inv_%'";
        var bodies=new ArrayList<String>();var urls=new ArrayList<String>();
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            var body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            bodies.add(body);urls.add(URLDecoder.decode(exchange.getRequestURI().getRawQuery(),StandardCharsets.UTF_8));
            assertEquals("private-test-only",exchange.getRequestHeaders().getFirst("X-ClickHouse-Key"));
            Object rows=body.contains("system.databases")?List.of(Map.of("name",namespace)):
                body.contains("system.columns")?List.of(Map.of("table","invoice","name","amount","type","Decimal(12, 2)","position",1,"is_in_sorting_key",0,"is_in_partition_key",0)):
                List.of(Map.of("name","invoice","engine","MergeTree"));
            byte[] bytes=json.writeValueAsBytes(Map.of("data",rows));exchange.sendResponseHeaders(200,bytes.length);
            exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        var result=connector.explore(config(namespace,prefix));
        assertEquals(false,result.get("truncated"));assertEquals(1,((List<?>)result.get("objects")).size());
        for(var body:bodies){assertFalse(body.contains(namespace));assertFalse(body.contains(prefix));assertTrue(body.contains("{namespace:String}"));}
        for(var url:urls){assertTrue(url.contains("param_namespace="+namespace));assertTrue(url.contains("param_prefix="+prefix));assertFalse(url.contains("private-test-only"));}
    }
    @Test void truncatesTablesAndColumnsAndAnnouncesPartialMetadata()throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            var body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            var rows=new ArrayList<Map<String,Object>>();
            if(body.contains("system.databases"))rows.add(Map.of("name","analytics"));
            else if(body.contains("system.columns"))for(int i=1;i<=201;i++)rows.add(Map.of("table","t0","name","c"+i,"type","String","position",i));
            else for(int i=0;i<101;i++)rows.add(Map.of("name","t"+i,"engine","MergeTree"));
            byte[] bytes=json.writeValueAsBytes(Map.of("data",rows));exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        var result=connector.explore(config("analytics",""));var objects=(List<?>)result.get("objects");
        assertEquals(100,objects.size());assertEquals(true,result.get("truncated"));
        var first=(Map<?,?>)objects.getFirst();assertEquals(true,first.get("columns_truncated"));assertEquals(200,((List<?>)first.get("columns")).size());
    }
    @Test void refusesRedirectsWithoutForwardingCredentials()throws Exception {
        var calls=new AtomicInteger();server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{calls.incrementAndGet();exchange.getResponseHeaders().set("Location","/leak");exchange.sendResponseHeaders(302,-1);exchange.close();});server.start();
        var error=assertThrows(NativeSourceException.class,()->connector.test(config("analytics","")));
        assertEquals(1,calls.get());assertFalse(error.getMessage().contains("private-test-only"));
    }
    @Test void refusesOversizedMetadataResponses()throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{byte[] body=new byte[2_000_001];exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});server.start();
        assertThrows(NativeSourceException.class,()->connector.test(config("analytics","")));
    }
    @Test void configuredClickhouseProfileNeverExposesItsPassword(){
        var profiles=new NativeSourceProfiles(new MockEnvironment().withProperty("CLICKHOUSE_READER_PASSWORD","private-profile-secret"));
        assertEquals("clickhouse",profiles.get("clickhouse-demo").engine());
        assertFalse(profiles.list().toString().contains("private-profile-secret"));assertFalse(profiles.list().getFirst().containsKey("password"));
    }
}
