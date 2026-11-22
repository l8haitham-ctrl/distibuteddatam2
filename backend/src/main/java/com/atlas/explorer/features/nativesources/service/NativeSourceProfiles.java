package com.atlas.explorer.features.nativesources.service;

import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.*;

@Service
public class NativeSourceProfiles {
    private final Map<String,NativeConnection> profiles;
    public NativeSourceProfiles(Environment env) {
        var map=new LinkedHashMap<String,NativeConnection>();
        String redis=env.getProperty("REDIS_READER_PASSWORD","");
        if(!redis.isBlank())map.put("redis-demo",new NativeConnection("redis","redis-source",6379,"0","atlas_reader",redis,false,"analytics:customer_segments:"));
        if(Boolean.parseBoolean(env.getProperty("storage.enabled","false"))) {
            var uri=URI.create(env.getProperty("storage.endpoint","http://minio:9000"));
            boolean tls="https".equals(uri.getScheme());
            map.put("minio-lake",new NativeConnection("minio",uri.getHost(),uri.getPort()<0?(tls?443:80):uri.getPort(),
                env.getProperty("storage.bucket","lake"),env.getProperty("storage.access-key",""),env.getProperty("storage.secret-key",""),tls,""));
        }
        profiles=Collections.unmodifiableMap(map);
    }
    public List<Map<String,Object>> list(){return profiles.entrySet().stream().map(entry->{
        var c=entry.getValue();return Map.<String,Object>of("id",entry.getKey(),"engine",c.engine(),"host",c.host(),"port",c.port(),
            "namespace",c.namespace(),"username",c.username(),"prefix",c.prefix(),"tls",c.tls());
    }).toList();}
    public NativeConnection get(String id){var config=profiles.get(id);if(config==null)throw new NativeSourceException();return config;}
}
