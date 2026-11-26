package com.atlas.explorer.features.nativeingestion;
import com.atlas.explorer.features.nativeingestion.connector.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.atlas.explorer.features.nativeingestion.dto.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisHashExtractionTests {
    @Test void decodesUtf8ExactlyAndRejectsBinaryInsteadOfReplacingCharacters(){
        assertEquals("é العربية",RedisHashExtraction.text("é العربية".getBytes(StandardCharsets.UTF_8)));
        assertNull(RedisHashExtraction.text(null));
        assertThrows(IllegalArgumentException.class,()->RedisHashExtraction.text(new byte[]{(byte)0xff,(byte)0xfe}));
        assertEquals("a\\*\\?\\[x\\]",RedisHashExtraction.literalPrefix("a*?[x]"));
    }
    @Test void validatesDatabaseAndDatasetBeforeConnecting(){
        var source=new RedisHashExtraction(Map.of("REDIS_READER_PASSWORD","test-only-secret","REDIS_DATABASE","16")::get);
        assertThrows(IllegalArgumentException.class,source::schema);
        var normal=new RedisHashExtraction(Map.of("REDIS_READER_PASSWORD","test-only-secret")::get);
        assertEquals("redis_0",normal.schema());assertEquals("customer_segments",normal.dataset());
        assertThrows(IllegalArgumentException.class,()->normal.describe("redis_0","any_other_dataset"));
    }
    @Test void rejectsDuplicateAndReservedFieldMappingsBeforeConnecting(){
        for(String fields:List.of("customer_id,CUSTOMER_ID","_key,segment","invalid-name")){
            var source=new RedisHashExtraction(Map.of("REDIS_READER_PASSWORD","test-only-secret","REDIS_EXTRACTION_FIELDS",fields)::get);
            assertThrows(IllegalArgumentException.class,()->source.describe("redis_0","customer_segments"));
        }
    }
    @Test void refusesNonHashesAndUnknownFieldsWithoutSilentlyDroppingData(){
        for(String type:List.of("string","hash")){
            try(var construction=mockConstruction(Jedis.class,(client,context)->{
                when(client.scan(any(byte[].class),any(ScanParams.class))).thenReturn(new ScanResult<>("0",List.of("analytics:customer_segments:1".getBytes(StandardCharsets.UTF_8))));
                when(client.type(any(byte[].class))).thenReturn(type);
                when(client.hrandfield(any(byte[].class),anyLong())).thenReturn(List.of("unmapped_field".getBytes(StandardCharsets.UTF_8)));
            })){
                var source=new RedisHashExtraction(Map.of("REDIS_READER_PASSWORD","test-only-secret")::get);
                var fields=List.of(new NativeField("_key","string",false),new NativeField("customer_id","string",true),new NativeField("segment","string",true),new NativeField("loyalty_points","string",true));
                try(var stream=source.rows(new NativeDataset("redis","redis_0","customer_segments",fields))){assertThrows(IllegalStateException.class,stream::hasNext);}
                verify(construction.constructed().getFirst()).close();
                verify(construction.constructed().getFirst(),never()).hmget(any(byte[].class),any(byte[][].class));
            }
        }
    }
}
