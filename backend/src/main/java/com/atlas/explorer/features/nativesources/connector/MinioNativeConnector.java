package com.atlas.explorer.features.nativesources.connector;

import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import io.minio.*;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.*;

@Component
public class MinioNativeConnector implements NativeConnector {
    private final OkHttpClient http=new OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(5)).callTimeout(Duration.ofSeconds(10)).build();
    public String engine(){return "minio";}
    public String displayName(){return "MinIO / S3";}
    public int defaultPort(){return 9000;}
    private MinioClient client(NativeConnection c) {
        String host=c.host().contains(":")?"["+c.host()+"]":c.host();
        return MinioClient.builder().endpoint((c.tls()?"https://":"http://")+host+":"+c.port())
            .credentials(c.username(),c.password()).httpClient(http).build();
    }
    public Map<String,Object> test(NativeConnection c) {
        try {
            if(!client(c).bucketExists(BucketExistsArgs.builder().bucket(c.namespace()).build()))throw new IllegalStateException();
            return Map.of("status","connected","engine",engine(),"namespace",c.namespace(),"transport",c.tls()?"TLS":"plain");
        }catch(Exception e){throw new com.atlas.explorer.features.nativesources.service.NativeSourceException();}
    }
    public Map<String,Object> explore(NativeConnection c) {
        try {
            long deadline=System.nanoTime()+10_000_000_000L;
            var objects=new ArrayList<Map<String,Object>>();boolean limited=false;
            var listing=client(c).listObjects(ListObjectsArgs.builder().bucket(c.namespace()).prefix(c.prefix()).recursive(true).maxKeys(100).build());
            for(var entry:listing) {
                if(objects.size()>=100 || System.nanoTime()>deadline){limited=true;break;}
                var item=entry.get();objects.add(Map.of("name",item.objectName(),"type","object","size",item.size(),"fields",List.of()));
            }
            return Map.of("engine",engine(),"namespace",c.namespace(),"objects",objects,"truncated",limited,"limit",100,
                "transport",c.tls()?"TLS":"plain","note","Liste de fichiers dans le bucket selectionne. Leur presence ne les enregistre pas automatiquement dans Trino.");
        }catch(Exception e){throw new com.atlas.explorer.features.nativesources.service.NativeSourceException();}
    }
}
