package com.atlas.explorer.features.nativeingestion;
import com.atlas.explorer.features.nativeingestion.connector.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_CLICKHOUSE_TESTS",matches="true")
class ClickhouseExtractionIntegrationTests {
    @Test void streamsEveryInvoiceWithExactDecimalStringsAndNoMetadataSampleLimit(){
        var sources=new NativeIngestionSources(System::getenv);var dataset=sources.describe("clickhouse","analytics","invoice");
        assertEquals("decimal(20,0)",dataset.fields().getFirst().type());
        int rows=0;BigDecimal sum=BigDecimal.ZERO;
        try(var stream=sources.rows(dataset)){while(stream.hasNext()){var row=stream.next();rows++;sum=sum.add(new BigDecimal(row.get(3)));}}
        assertEquals(5000,rows);assertTrue(sum.signum()>0);
    }
    @Test void retainsNullsAndRejectsUnknownTablesAndNamespaces(){
        var sources=new NativeIngestionSources(System::getenv);var dataset=sources.describe("clickhouse","analytics","customer");
        int rows=0,nulls=0;try(var stream=sources.rows(dataset)){while(stream.hasNext()){var row=stream.next();rows++;if(row.get(3)==null)nulls++;}}
        assertEquals(1000,rows);assertEquals(100,nulls);
        assertThrows(IllegalArgumentException.class,()->sources.describe("clickhouse","analytics","missing"));
        assertThrows(IllegalArgumentException.class,()->sources.describe("clickhouse","system","columns"));
    }
}
