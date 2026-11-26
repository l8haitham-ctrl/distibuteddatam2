package com.atlas.explorer.features.nativesources.service;

import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.*;

@Service
public class NativeSourceProfiles {
    private final Map<String,NativeConnection> profiles;
    private final String redisDataset;
    public NativeSourceProfiles(Environment env) {
        redisDataset=env.getProperty("REDIS_EXTRACTION_DATASET","customer_segments");
        var map=new LinkedHashMap<String,NativeConnection>();
        String redis=env.getProperty("REDIS_READER_PASSWORD","");
        if(!redis.isBlank())map.put("redis-demo",new NativeConnection("redis",env.getProperty("REDIS_HOST","redis-source"),Integer.parseInt(env.getProperty("REDIS_PORT","6379")),env.getProperty("REDIS_DATABASE","0"),env.getProperty("REDIS_USERNAME","atlas_reader"),redis,Boolean.parseBoolean(env.getProperty("REDIS_TLS","false")),env.getProperty("REDIS_EXTRACTION_PREFIX","analytics:customer_segments:")));
        String clickhouse=env.getProperty("CLICKHOUSE_READER_PASSWORD","");
        if(!clickhouse.isBlank())map.put("clickhouse-demo",new NativeConnection("clickhouse","clickhouse-source",8123,"analytics","atlas_reader",clickhouse,false,""));
        if(Boolean.parseBoolean(env.getProperty("storage.enabled","false"))) {
            var uri=URI.create(env.getProperty("storage.endpoint","http://minio:9000"));
            boolean tls="https".equals(uri.getScheme());
            map.put("minio-lake",new NativeConnection("minio",uri.getHost(),uri.getPort()<0?(tls?443:80):uri.getPort(),
                env.getProperty("storage.bucket","lake"),env.getProperty("storage.access-key",""),env.getProperty("storage.secret-key",""),tls,""));
        }
        profiles=Collections.unmodifiableMap(map);
    }
    public List<Map<String,Object>> list(){return profiles.entrySet().stream().map(entry->{
        var c=entry.getValue();Map<String,Object> row=new LinkedHashMap<>(Map.<String,Object>of("id",entry.getKey(),"engine",c.engine(),"host",c.host(),"port",c.port(),
            "namespace",c.namespace(),"username",c.username(),"prefix",c.prefix(),"tls",c.tls()));
        if(c.engine().equals("clickhouse"))row.put("extraction_source_id","clickhouse");
        if(c.engine().equals("redis")){row.put("extraction_source_id","redis");row.put("extraction_schema","redis_"+Integer.parseInt(c.namespace()));row.put("extraction_dataset",redisDataset);}
        return row;
    }).toList();}
    public NativeConnection get(String id){var config=profiles.get(id);if(config==null)throw new NativeSourceException();return config;}
}
