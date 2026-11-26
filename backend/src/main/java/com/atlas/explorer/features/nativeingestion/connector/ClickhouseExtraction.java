package com.atlas.explorer.features.nativeingestion.connector;

import com.atlas.explorer.features.nativeingestion.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Only trusted server/worker configuration supplies credentials. No HTTP framework dependencies. */
public final class ClickhouseExtraction {
    private final Function<String,String> env;
    private final ObjectMapper json=new ObjectMapper();
    private final OkHttpClient client=new OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(30)).callTimeout(Duration.ofMinutes(10))
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
    public ClickhouseExtraction(Function<String,String> env){this.env=env;}
    public boolean enabled(){return env.apply("CLICKHOUSE_READER_PASSWORD")!=null&&!env.apply("CLICKHOUSE_READER_PASSWORD").isBlank();}
    public String database(){return value("CLICKHOUSE_DATABASE","analytics");}
    private String value(String key,String fallback){String value=env.apply(key);return value==null||value.isBlank()?fallback:value;}
    public static String quote(String name){if(name==null||!name.matches("[A-Za-z_][A-Za-z0-9_]{0,127}"))throw new IllegalArgumentException("Identifiant d'extraction invalide.");return "\""+name+"\"";}
    private Call call(String sql,boolean metadata,String schema,String table){
        if(!enabled())throw new IllegalArgumentException("Source ClickHouse non configuree.");
        var url=new HttpUrl.Builder().scheme(Boolean.parseBoolean(value("CLICKHOUSE_TLS","false"))?"https":"http")
            .host(value("CLICKHOUSE_HOST","clickhouse-source")).port(Integer.parseInt(value("CLICKHOUSE_HTTP_PORT","8123")))
            .addQueryParameter("param_schema",schema).addQueryParameter("param_table",table).build();
        var call=client.newCall(new Request.Builder().url(url).header("X-ClickHouse-User",value("CLICKHOUSE_USERNAME","atlas_reader"))
            .header("X-ClickHouse-Key",value("CLICKHOUSE_READER_PASSWORD",""))
            .post(RequestBody.create(sql,MediaType.get("text/plain; charset=utf-8"))).build());
        if(metadata)call.timeout().timeout(10,TimeUnit.SECONDS);return call;
    }
    public static NativeField field(String name,String original){
        quote(name);String type=original;boolean nullable=false;
        while(type.startsWith("Nullable(")||type.startsWith("LowCardinality(")){
            nullable|=type.startsWith("Nullable(");type=type.substring(type.indexOf('(')+1,type.length()-1);
        }
        String mapped=switch(type){
            case "String","UUID","IPv4","IPv6"->"string";
            case "Int8"->"byte";case "UInt8","Int16"->"short";case "UInt16","Int32"->"integer";
            case "UInt32","Int64"->"long";case "UInt64"->"decimal(20,0)";
            case "Float32"->"float";case "Float64"->"double";case "Bool"->"boolean";
            case "Date","Date32"->"date";case "DateTime"->"timestamp";
            default->{
                if(type.startsWith("FixedString("))yield "string";
                if(type.startsWith("DateTime("))yield "timestamp";
                var decimal=Pattern.compile("Decimal\\(([0-9]+),\\s*([0-9]+)\\)").matcher(type);
                if(decimal.matches()&&Integer.parseInt(decimal.group(1))<=38&&Integer.parseInt(decimal.group(2))<=Integer.parseInt(decimal.group(1)))
                    yield "decimal("+decimal.group(1)+","+decimal.group(2)+")";
                throw new IllegalArgumentException("Type ClickHouse non pris en charge pour une extraction sans perte.");
            }
        };return new NativeField(name,mapped,nullable);
    }
    public NativeDataset describe(String schema,String table){
        quote(schema);quote(table);if(!database().equals(schema))throw new IllegalArgumentException("Base ClickHouse non autorisee.");
        String sql="SELECT name,type FROM system.columns WHERE database={schema:String} AND table={table:String} ORDER BY position LIMIT 1025 FORMAT JSON";
        try(var response=call(sql,true,schema,table).execute()){
            if(!response.isSuccessful()||response.body()==null)throw new IllegalArgumentException();
            byte[] bytes=response.body().byteStream().readNBytes(2_000_001);if(bytes.length>2_000_000)throw new IllegalArgumentException();
            var rows=json.readTree(bytes).path("data");if(!rows.isArray()||rows.isEmpty()||rows.size()>1024)throw new IllegalArgumentException();
            var fields=new ArrayList<NativeField>();var names=new HashSet<String>();
            for(var row:rows){var field=field(row.path("name").asText(),row.path("type").asText());if(!names.add(field.name().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException();fields.add(field);}
            return new NativeDataset("clickhouse",schema,table,fields);
        }catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Lecture de la structure ClickHouse impossible.");}
    }
    public RowStream rows(NativeDataset dataset){
        var selected=dataset.fields().stream().map(f->(f.type().equals("timestamp")?
            "formatDateTime("+quote(f.name())+",'%Y-%m-%d %H:%i:%s','UTC')":"toString("+quote(f.name())+")")+" AS "+quote(f.name())).toList();
        String sql="SELECT "+String.join(",",selected)+" FROM "+quote(dataset.schema())+"."+quote(dataset.table())+" FORMAT JSONEachRow";
        try{
            var response=call(sql,false,dataset.schema(),dataset.table()).execute();
            if(!response.isSuccessful()||response.body()==null){response.close();throw new IllegalArgumentException("Lecture ClickHouse refusee.");}
            return new RowStream(){
                List<String> next;boolean ended;
                public boolean hasNext(){
                    if(next!=null)return true;if(ended)return false;
                    try{
                        var stream=response.body().source();if(stream.exhausted()){close();return false;}
                        String line=stream.readUtf8LineStrict(4_194_304);var record=json.readTree(line);
                        next=new ArrayList<>();for(var field:dataset.fields()){
                            if(!record.has(field.name()))throw new IllegalArgumentException("Schema ClickHouse modifie pendant l'extraction.");
                            var value=record.get(field.name());if(value.isNull()&&!field.nullable())throw new IllegalArgumentException("NULL inattendu.");
                            next.add(value.isNull()?null:value.asText());
                        }return true;
                    }catch(Exception e){close();throw new IllegalStateException("Lecture ClickHouse interrompue ou schema incompatible.");}
                }
                public List<String> next(){if(!hasNext())throw new NoSuchElementException();var row=next;next=null;return row;}
                public void close(){ended=true;response.close();}
            };
        }catch(Exception e){throw new IllegalArgumentException("Ouverture du flux ClickHouse impossible.");}
    }
}
