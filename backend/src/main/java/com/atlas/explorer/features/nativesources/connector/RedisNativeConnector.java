package com.atlas.explorer.features.nativesources.connector;

import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;
import redis.clients.jedis.params.ScanParams;
import javax.net.ssl.SSLParameters;
import java.util.*;

@Component
public class RedisNativeConnector implements NativeConnector {
    public String engine(){return "redis";}
    public String displayName(){return "Redis";}
    public int defaultPort(){return 6379;}
    private Jedis open(NativeConnection c) {
        if(!c.namespace().matches("[0-9]{1,2}") || Integer.parseInt(c.namespace())>15)throw new IllegalArgumentException("Redis database index must be 0..15");
        var ssl=new SSLParameters();ssl.setEndpointIdentificationAlgorithm("HTTPS");
        var config=DefaultJedisClientConfig.builder().user(c.username()).password(c.password())
            .database(Integer.parseInt(c.namespace())).ssl(c.tls()).sslParameters(ssl)
            .connectionTimeoutMillis(3000).socketTimeoutMillis(3000).build();
        return new Jedis(new HostAndPort(c.host(),c.port()),config);
    }
    public Map<String,Object> test(NativeConnection c) {
        try(var client=open(c)) {
            if(!"PONG".equals(client.ping()))throw new IllegalStateException();
            return Map.of("status","connected","engine",engine(),"namespace",c.namespace(),"transport",c.tls()?"TLS":"plain");
        }
    }
    public Map<String,Object> explore(NativeConnection c) {
        long deadline=System.nanoTime()+10_000_000_000L;
        try(var client=open(c)) {
            var objects=new ArrayList<Map<String,Object>>();var seen=new HashSet<String>();
            String cursor="0";boolean limited=false;int pages=0;
            var params=new ScanParams().match(literalPrefix(c.prefix())+"*").count(100);
            outer: do {
                var scan=client.scan(cursor,params);cursor=scan.getCursor();pages++;
                for(String key:scan.getResult()) {
                    if(!seen.add(key))continue;
                    if(objects.size()>=100 || System.nanoTime()>deadline){limited=true;break outer;}
                    String type=client.type(key);
                    if("none".equals(type))continue; // Key may expire between SCAN and TYPE.
                    List<String> fields="hash".equals(type)?client.hrandfield(key,20):List.of();
                    objects.add(Map.of("name",key,"type",type,"fields",fields));
                }
                if((pages>=20 || System.nanoTime()>deadline) && !"0".equals(cursor)){limited=true;break;}
            } while(!"0".equals(cursor));
            return Map.of("engine",engine(),"namespace",c.namespace(),"objects",objects,"truncated",limited,
                "limit",100,"transport",c.tls()?"TLS":"plain","note","Echantillon de cles ; au plus 20 noms de champs par hash. Aucune valeur chargee. SCAN ne fournit pas de snapshot.");
        }
    }
    public static String literalPrefix(String prefix){return prefix.replace("\\","\\\\").replace("*","\\*").replace("?","\\?").replace("[","\\[").replace("]","\\]");}
}
