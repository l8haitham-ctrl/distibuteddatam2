package com.atlas.explorer.features.nativesources.connector;

import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import com.atlas.explorer.features.nativesources.service.NativeSourceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.*;

/** Bounded metadata exploration using parameterized SELECTs over the ClickHouse HTTP interface. */
@Component
public class ClickhouseNativeConnector implements NativeConnector {
    private static final int TABLE_LIMIT=100, COLUMN_LIMIT=200, RESPONSE_LIMIT=2_000_000;
    private static final String TABLE_FILTER="database={namespace:String} AND startsWith(name,{prefix:String})";
    private final ObjectMapper json=new ObjectMapper();
    private final OkHttpClient http=new OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(5)).callTimeout(Duration.ofSeconds(5))
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
    public String engine(){return "clickhouse";}
    public String displayName(){return "ClickHouse (OLAP)";}
    public int defaultPort(){return 8123;}

    private JsonNode query(NativeConnection c,String sql) {
        try {
            var url=new HttpUrl.Builder().scheme(c.tls()?"https":"http").host(c.host()).port(c.port())
                .addQueryParameter("param_namespace",c.namespace()).addQueryParameter("param_prefix",c.prefix()).build();
            var request=new Request.Builder().url(url).header("X-ClickHouse-User",c.username())
                .header("X-ClickHouse-Key",c.password()).post(RequestBody.create(sql,MediaType.get("text/plain; charset=utf-8"))).build();
            try(var response=http.newCall(request).execute()) {
                if(!response.isSuccessful()||response.body()==null)throw new NativeSourceException();
                byte[] bytes=response.body().byteStream().readNBytes(RESPONSE_LIMIT+1);
                if(bytes.length>RESPONSE_LIMIT)throw new NativeSourceException();
                var data=json.readTree(bytes).path("data");
                if(!data.isArray())throw new NativeSourceException();
                return data;
            }
        }catch(Exception e){throw new NativeSourceException();}
    }
    private void verifyDatabase(NativeConnection c) {
        var databases=query(c,"SELECT name FROM system.databases WHERE name={namespace:String} LIMIT 1 FORMAT JSON");
        if(databases.size()!=1||!c.namespace().equals(databases.get(0).path("name").asText()))throw new NativeSourceException();
    }
    public Map<String,Object> test(NativeConnection c) {
        verifyDatabase(c);
        return Map.of("status","connected","engine",engine(),"namespace",c.namespace(),"transport",c.tls()?"TLS":"plain");
    }
    public Map<String,Object> explore(NativeConnection c) {
        verifyDatabase(c);
        var tables=query(c,"SELECT name,engine FROM system.tables WHERE "+TABLE_FILTER+" ORDER BY name LIMIT 101 FORMAT JSON");
        if(tables.size()>101)throw new NativeSourceException();
        var columns=query(c,"""
            SELECT table,name,type,position,is_in_sorting_key,is_in_partition_key
            FROM system.columns WHERE database={namespace:String} AND table IN (
                SELECT name FROM system.tables WHERE %s ORDER BY name LIMIT 100)
            ORDER BY table,position LIMIT 201 BY table LIMIT 20100 FORMAT JSON
            """.formatted(TABLE_FILTER));
        if(columns.size()>20100)throw new NativeSourceException();
        var byTable=new HashMap<String,List<Map<String,Object>>>();
        for(var column:columns) {
            byTable.computeIfAbsent(column.path("table").asText(),key->new ArrayList<>()).add(Map.of(
                "name",column.path("name").asText(),"data_type",column.path("type").asText(),
                "position",column.path("position").asInt(),"sorting_key",column.path("is_in_sorting_key").asInt()==1,
                "partition_key",column.path("is_in_partition_key").asInt()==1));
        }
        var objects=new ArrayList<Map<String,Object>>();boolean limited=tables.size()>TABLE_LIMIT;
        for(int index=0;index<Math.min(tables.size(),TABLE_LIMIT);index++) {
            var table=tables.get(index);String name=table.path("name").asText();
            var all=byTable.getOrDefault(name,List.of());boolean truncated=all.size()>COLUMN_LIMIT;
            limited|=truncated;var visible=all.subList(0,Math.min(all.size(),COLUMN_LIMIT));
            objects.add(Map.of("name",name,"type",table.path("engine").asText(),"fields",visible.stream().map(v->v.get("name")).toList(),
                "columns",visible,"columns_truncated",truncated));
        }
        return Map.of("engine",engine(),"namespace",c.namespace(),"objects",objects,"truncated",limited,
            "limit",TABLE_LIMIT,"transport",c.tls()?"TLS":"plain","note",
            "Metadonnees uniquement : 100 tables et 200 colonnes par table au maximum. Les cles de tri ClickHouse ne garantissent pas l'unicite. Aucune ligne metier chargee.");
    }
}
