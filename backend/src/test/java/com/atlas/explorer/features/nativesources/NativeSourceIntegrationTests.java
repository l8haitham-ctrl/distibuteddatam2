package com.atlas.explorer.features.nativesources;

import com.atlas.explorer.features.nativesources.connector.*;
import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import com.atlas.explorer.features.nativesources.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_NATIVE_TESTS",matches="true")
class NativeSourceIntegrationTests {
    private final NativeSourceService service=new NativeSourceService(List.of(new RedisNativeConnector(),new MinioNativeConnector()));
    private NativeConnection redis(String password){return new NativeConnection("redis","redis-source",6379,"0","atlas_reader",password,false,"analytics:customer_segments:");}
    private NativeConnection minio(String password){return new NativeConnection("minio","minio",9000,System.getenv("STORAGE_BUCKET"),System.getenv("STORAGE_ACCESS_KEY"),password,false,"postgresql/public/invoice/");}
    @Test void redisConnectionAndBoundedStructureExploration() {
        var c=redis(System.getenv("REDIS_READER_PASSWORD"));
        assertEquals("connected",service.test(c).get("status"));
        var result=service.explore(c);var objects=(List<?>)result.get("objects");
        assertEquals(100,objects.size());assertEquals(true,result.get("truncated"));
        var first=(Map<?,?>)objects.getFirst();assertEquals("hash",first.get("type"));
        assertEquals(Set.of("customer_id","segment","loyalty_points"),new HashSet<>((List<?>)first.get("fields")));
        assertFalse(first.containsKey("password"));assertFalse(first.containsKey("values"));
    }
    @Test void minioConnectionAndObjectExploration() {
        var c=minio(System.getenv("STORAGE_SECRET_KEY"));
        assertEquals("connected",service.test(c).get("status"));
        var objects=(List<?>)service.explore(c).get("objects");
        assertFalse(objects.isEmpty());assertTrue(objects.size()<=100);
        assertTrue(objects.stream().map(o->(Map<?,?>)o).anyMatch(o->o.get("name").toString().endsWith(".parquet")));
    }
    @Test void rejectedCredentialsAreSanitizedForBothEngines() {
        for(var c:List.of(redis("wrong-secret-value"),minio("wrong-secret-value"))) {
            var error=assertThrows(NativeSourceException.class,()->service.test(c));
            assertFalse(error.getMessage().contains("wrong-secret"));
        }
    }
    @Test void invalidRedisDatabaseIsRejectedWithoutConnecting() {
        var c=new NativeConnection("redis","redis-source",6379,"16","atlas_reader",System.getenv("REDIS_READER_PASSWORD"),false,"");
        assertThrows(NativeSourceException.class,()->service.test(c));
    }
}
