package com.atlas.explorer.features.nativeingestion;
import com.atlas.explorer.features.nativeingestion.connector.*;
import com.atlas.explorer.features.nativeingestion.dto.*;
import com.atlas.explorer.features.jobs.service.*;
import com.atlas.explorer.features.jobs.connector.*;
import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClickhouseExtractionTests {
    @Test void retainsUnsignedDecimalsNullableAndTemporalTypesWithoutGuessing(){
        assertEquals("decimal(20,0)",ClickhouseExtraction.field("id","UInt64").type());
        assertEquals("decimal(12,2)",ClickhouseExtraction.field("amount","Decimal(12, 2)").type());
        assertTrue(ClickhouseExtraction.field("email","LowCardinality(Nullable(String))").nullable());
        assertEquals("timestamp",ClickhouseExtraction.field("created_at","DateTime('Europe/Paris')").type());
        for(String type:List.of("Array(String)","Map(String, String)","DateTime64(6)","Int128","Decimal(76, 2)"))
            assertThrows(IllegalArgumentException.class,()->ClickhouseExtraction.field("unsupported",type));
        assertThrows(IllegalArgumentException.class,()->ClickhouseExtraction.quote("table;DROP"));
    }
    @Test void validatesNativeSchemaBeforePersistingAndKeepsExistingJdbcPathSeparate(){
        var repository=mock(JobRepository.class);var sql=mock(SourceService.class);var exploration=mock(ExplorationService.class);
        var nativeSources=mock(NativeIngestionSources.class);when(nativeSources.supports("clickhouse")).thenReturn(true);when(nativeSources.defaultSchema("clickhouse")).thenReturn("analytics");
        when(nativeSources.describe("clickhouse","analytics","invoice")).thenReturn(new NativeDataset("clickhouse","analytics","invoice",List.of(new NativeField("amount","decimal(12,2)",false))));
        var service=new JobService(repository,sql,exploration,mock(PartitionCardinality.class),nativeSources);
        service.create(new CreateJobRequest("clickhouse",null,"invoice",null,null,null,null));
        verify(repository).create(argThat(request->request.schema().equals("analytics")));verifyNoInteractions(sql,exploration);
        assertThrows(InvalidJobException.class,()->service.create(new CreateJobRequest("clickhouse","analytics","invoice","parquet",List.of("amount"),1000,"snappy")));
        verify(repository,times(1)).create(any());
    }
}
