package com.atlas.explorer;
import com.atlas.explorer.features.catalog.service.*;
import com.atlas.explorer.features.jobs.connector.*;
import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.features.jobs.service.JobDispatcher;
import com.atlas.explorer.features.jobs.config.JobProperties;
import com.atlas.explorer.features.storage.service.StorageService;
import com.atlas.explorer.features.sql.service.*;
import java.util.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CatalogTests {
    private Map<String,Object> manifest() {
        var m=new HashMap<String,Object>();m.put("source_id","sqlserver");m.put("schema_name","dbo");m.put("table_name","Invoice");
        m.put("bucket","lake");m.put("prefix","sqlserver/dbo/Invoice/1234");m.put("format","parquet");
        m.put("schema_json","{\"fields\":[{\"name\":\"amount\",\"type\":\"decimal(12,2)\"},{\"name\":\"invoice_date\",\"type\":\"date\"},{\"name\":\"invoice_date_year\",\"type\":\"integer\"}]}");
        m.put("partition_columns",List.of(Map.of("name","invoice_date_year","derived",true)));return m;
    }
    @Test void tableDefinitionKeepsTypedDataAndPlacesPartitionsLast() {
        var d=new TableDefinition(UUID.fromString("12345678-0000-0000-0000-000000000000"),manifest());
        assertEquals("hive.runs.invoice_12345678",d.table);assertEquals("hive.sqlserver_dbo.invoice",d.view);
        assertTrue(d.matchesLocation("external_location = '"+d.location.replaceFirst("/$","")+"'"));
        assertFalse(d.matchesLocation("external_location = '"+d.location+"another-run'"));
        assertTrue(d.columns.endsWith("\"invoice_date_year\" INTEGER"));assertTrue(d.columns.contains("DECIMAL(12,2)"));
        assertEquals("\"amount\",\"invoice_date\"",d.projection);assertTrue(d.properties.contains("partitioned_by=ARRAY['invoice_date_year']"));
        var csv=manifest();csv.put("format","csv");var text=new TableDefinition(UUID.randomUUID(),csv);
        assertTrue(text.properties.contains("format='TEXTFILE'"));assertTrue(text.properties.contains("null_format='\\N'"));
        csv.put("schema_json","{\"fields\":[{\"name\":\"private-invalid!\",\"type\":\"string\"}]}");assertThrows(CatalogException.class,()->new TableDefinition(UUID.randomUUID(),csv));
        assertThrows(CatalogException.class,()->TableDefinition.sqlType("array<string>"));
    }
    @Test void registeringJobIsNotCompletedWhenCatalogRegistrationFails() {
        var repo=mock(JobRepository.class);var engine=mock(JobEngine.class);var storage=mock(StorageService.class);var catalog=mock(CatalogService.class);
        var request=new CreateJobRequest("sqlserver",null,"Invoice",null,null,null,null);
        var job=new StorageJob(UUID.randomUUID(),"REGISTERING",request,Instant.now(),Instant.now(),null,"driver-test-1",null,null);
        var manifest=manifest();when(repo.active()).thenReturn(List.of(job));when(repo.pending(job.id())).thenReturn(manifest);
        when(repo.claim()).thenReturn(Optional.empty());when(catalog.register(job,manifest)).thenThrow(new CatalogException());
        when(catalog.discard(job,manifest)).thenReturn(true);
        var props=new JobProperties("jdbc:postgresql://metadata/atlas_test","atlas","hidden",1800,"http://spark-master:6066","spark://spark-master:7077");
        new JobDispatcher(repo,engine,storage,props,catalog).tick();
        verify(catalog).discard(job,manifest);verify(repo).failed(job.id(),"CATALOG_REGISTRATION_FAILED");verify(repo,never()).completed(any(),any());verifyNoInteractions(engine,storage);
    }
    @Test void sqlInputHasAConservativeSingleStatementContract() {
        assertEquals("SELECT 1",SqlService.validate("SELECT 1; "));
        assertEquals("WITH x AS (SELECT 1) SELECT * FROM x",SqlService.validate("WITH x AS (SELECT 1) SELECT * FROM x"));
        for(String sql:List.of("DROP TABLE hive.runs.invoice","SELECT 1; DELETE FROM t","-- comment\nSELECT 1","/* comment */ SELECT 1"))
            assertThrows(SqlException.class,()->SqlService.validate(sql));
    }
}
