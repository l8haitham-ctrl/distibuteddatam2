package com.atlas.explorer.features.nativeingestion.connector;

import com.atlas.explorer.features.nativeingestion.dto.*;
import redis.clients.jedis.*;
import redis.clients.jedis.params.ScanParams;
import javax.net.ssl.SSLParameters;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.function.Function;

/** A trusted, explicit hash mapping; no inference from a 100-key exploration sample. */
public final class RedisHashExtraction {
    private final Function<String,String> env;
    public RedisHashExtraction(Function<String,String> env){this.env=env;}
    private String value(String key,String fallback){String value=env.apply(key);return value==null||value.isBlank()?fallback:value;}
    public boolean enabled(){return !value("REDIS_READER_PASSWORD","").isBlank();}
    public String schema(){String database=value("REDIS_DATABASE","0");if(!database.matches("[0-9]{1,2}")||Integer.parseInt(database)>15)throw new IllegalArgumentException("Index Redis invalide.");return "redis_"+Integer.parseInt(database);}
    public String dataset(){String table=value("REDIS_EXTRACTION_DATASET","customer_segments");ClickhouseExtraction.quote(table);return table;}
    private String prefix(){String prefix=value("REDIS_EXTRACTION_PREFIX","analytics:customer_segments:");if(prefix.isBlank())throw new IllegalArgumentException("Un prefixe Redis est obligatoire.");return prefix;}
    private List<String> fields(){
        var fields=Arrays.stream(value("REDIS_EXTRACTION_FIELDS","customer_id,segment,loyalty_points").split(",",-1)).map(String::trim).toList();
        if(fields.isEmpty()||fields.size()>128)throw new IllegalArgumentException("Mapping Redis invalide.");
        var seen=new HashSet<String>();seen.add("_key");for(String field:fields){ClickhouseExtraction.quote(field);if(!seen.add(field.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Mapping Redis ambigu.");}
        return fields;
    }
    private Jedis open(){
        var tls=new SSLParameters();tls.setEndpointIdentificationAlgorithm("HTTPS");
        var config=DefaultJedisClientConfig.builder().user(value("REDIS_USERNAME","atlas_reader")).password(value("REDIS_READER_PASSWORD",""))
            .database(Integer.parseInt(schema().substring(6))).ssl(Boolean.parseBoolean(value("REDIS_TLS","false"))).sslParameters(tls)
            .connectionTimeoutMillis(3000).socketTimeoutMillis(3000).build();
        return new Jedis(new HostAndPort(value("REDIS_HOST","redis-source"),Integer.parseInt(value("REDIS_PORT","6379"))),config);
    }
    public NativeDataset describe(String schema,String table){
        if(!enabled()||!schema().equals(schema)||!dataset().equals(table))throw new IllegalArgumentException("Dataset Redis inconnu.");
        prefix();var mapped=new ArrayList<NativeField>();mapped.add(new NativeField("_key","string",false));fields().forEach(field->mapped.add(new NativeField(field,"string",true)));
        try(var client=open()){if(!"PONG".equals(client.ping()))throw new IllegalStateException();}
        catch(Exception error){throw new IllegalArgumentException("Connexion Redis impossible.");}
        return new NativeDataset("redis",schema,table,mapped);
    }
    public static String text(byte[] bytes){
        if(bytes==null)return null;
        try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
        catch(CharacterCodingException error){throw new IllegalArgumentException("Les valeurs Redis binaires non UTF-8 ne sont pas prises en charge.");}
    }
    public static String literalPrefix(String prefix){return prefix.replace("\\","\\\\").replace("*","\\*").replace("?","\\?").replace("[","\\[").replace("]","\\]");}
    public RowStream rows(NativeDataset specification){
        if(!schema().equals(specification.schema())||!dataset().equals(specification.table())||!fields().equals(specification.fields().stream().skip(1).map(NativeField::name).toList()))throw new IllegalArgumentException("Mapping Redis modifie.");
        var client=open();var names=fields();byte[][] requested=names.stream().map(name->name.getBytes(StandardCharsets.UTF_8)).toArray(byte[][]::new);
        var params=new ScanParams().match(literalPrefix(prefix())+"*").count(100);
        long deadline=System.nanoTime()+600_000_000_000L;
        return new RowStream(){
            String cursor="0";boolean started,closed;final ArrayDeque<byte[]> keys=new ArrayDeque<>();List<String> next;
            public boolean hasNext(){
                if(next!=null)return true;if(closed)return false;
                try{
                    while(true){
                        if(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new IllegalStateException();
                        if(keys.isEmpty()){
                            if(started&&cursor.equals("0")){close();return false;}
                            var scan=client.scan(cursor.getBytes(StandardCharsets.US_ASCII),params);started=true;cursor=scan.getCursor();keys.addAll(scan.getResult());continue;
                        }
                        byte[] key=keys.removeFirst();if(key.length>4096)throw new IllegalArgumentException();String name=text(key),type=client.type(key);
                        if(type.equals("none"))continue;if(!type.equals("hash"))throw new IllegalArgumentException();
                        var present=client.hrandfield(key,names.size()+1L);
                        if(present.size()>names.size()||present.stream().map(RedisHashExtraction::text).anyMatch(field->!names.contains(field)))throw new IllegalArgumentException();
                        long bytes=key.length;for(byte[] field:requested)bytes+=client.hstrlen(key,field);
                        if(bytes>4_194_304)throw new IllegalArgumentException();
                        var values=client.hmget(key,requested);if(!client.exists(key))continue;
                        var row=new ArrayList<String>();row.add(name);long actual=key.length;
                        for(byte[] value:values){actual+=value==null?0:value.length;if(actual>4_194_304)throw new IllegalArgumentException();row.add(text(value));}
                        next=row;return true;
                    }
                }catch(Exception error){close();throw new IllegalStateException("Lecture Redis interrompue : type, mapping, taille ou encodage incompatible.");}
            }
            public List<String> next(){if(!hasNext())throw new NoSuchElementException();var row=next;next=null;return row;}
            public void close(){if(!closed){closed=true;client.close();}}
        };
    }
}
