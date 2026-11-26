package com.atlas.explorer.features.nativeingestion;
import com.atlas.explorer.features.nativeingestion.connector.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_NATIVE_TESTS",matches="true")
class RedisHashExtractionIntegrationTests {
    @Test void scansTheWholePrefixWithValuesBeyondTheExplorationSample(){
        var sources=new NativeIngestionSources(System::getenv);var dataset=sources.describe("redis","redis_0","customer_segments");
        assertEquals(List.of("_key","customer_id","segment","loyalty_points"),dataset.fields().stream().map(field->field.name()).toList());
        var keys=new HashSet<String>();long ids=0,points=0;int vip=0;
        try(var stream=sources.rows(dataset)){while(stream.hasNext()){var row=stream.next();if(!keys.add(row.getFirst()))continue;ids+=Long.parseLong(row.get(1));points+=Long.parseLong(row.get(3));if(row.get(2).equals("vip"))vip++;}}
        assertEquals(1000,keys.size());assertEquals(500500,ids);assertEquals(5005000,points);assertEquals(200,vip);
    }
    @Test void refusesInvalidCredentialsWithoutReturningThem(){
        var env=new HashMap<>(System.getenv());env.put("REDIS_READER_PASSWORD","wrong-private-secret");
        var error=assertThrows(IllegalArgumentException.class,()->new NativeIngestionSources(env::get).describe("redis","redis_0","customer_segments"));
        assertFalse(error.getMessage().contains("wrong-private-secret"));
    }
}
