package com.atlas.explorer.features.quality;
import com.atlas.explorer.features.quality.connector.QualityQueries;
import com.atlas.explorer.features.quality.service.*;
import com.atlas.explorer.features.quality.dto.QualityRequest;
import com.atlas.explorer.features.quality.api.QualityController;
import com.atlas.explorer.features.jobs.service.JobService;
import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connectors.connector.PluginRegistry;
import com.atlas.explorer.features.quality.connector.QualityReader;
import com.atlas.explorer.features.catalog.connector.TrinoConnections;
import com.atlas.explorer.shared.api.ApiExceptionHandler;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class QualityTests {
    private Map<String,Map<String,String>> metrics(){
        var values=new LinkedHashMap<String,Map<String,String>>();
        for(String table:List.of("customer","invoice","payment"))values.put(table,new LinkedHashMap<>(Map.of("row_count","2","distinct_ids","2","id_sum","3","null_rows","0","invalid_timestamps","0")));
        values.get("customer").putAll(Map.of("duplicate_emails","0","invalid_emails","0"));
        values.get("invoice").putAll(Map.of("amount_sum","20.00","invalid_amounts","0","invalid_statuses","0"));
        values.get("payment").putAll(Map.of("amount_sum","10.00","invalid_amounts","0","invalid_methods","0"));
        for(String r:List.of("orphan_invoices","orphan_payments","payment_dates","invoice_balances"))values.put(r,Map.of("violations","0"));
        return values;
    }
    @Test void rulesAcceptExactDecimalsAndRejectCorruptionAndSourceDrift(){
        var before=metrics();var after=metrics();var lake=metrics();lake.get("invoice").put("amount_sum","20.0000");
        assertTrue(QualityRules.evaluate(before,after,lake).stream().allMatch(c->c.get("status").equals("passed")));
        lake.get("invoice").put("amount_sum","19.99");lake.get("customer").put("null_rows","1");after.get("payment").put("row_count","3");
        var failed=QualityRules.evaluate(before,after,lake).stream().filter(c->c.get("status").equals("failed")).map(c->c.get("id")).toList();
        assertTrue(failed.containsAll(List.of("reconciliation.invoice.amount_sum","lake.customer.null_rows","source_stability.payment.row_count")));
    }
    @Test void queriesIncludeNullsKeysRelationshipsAndBusinessRules(){
        var sql=QualityQueries.build(n->"demo."+n);
        assertEquals(7,sql.size());assertTrue(sql.get("customer").contains("email IS NULL"));
        assertTrue(sql.get("invoice").contains("COUNT(DISTINCT invoice_id)"));
        assertTrue(sql.get("invoice_balances").contains("p.paid>=i.amount"));assertTrue(sql.get("payment_dates").contains("p.payment_date<i.invoice_date"));
    }
    @Test void serviceRejectsMixedSourcesAndSqlIdentifiersBeforeOpeningConnections(){
        var jobs=mock(JobService.class);var sources=mock(SourceService.class);var trino=mock(TrinoConnections.class);var reader=mock(QualityReader.class);
        when(sources.get("sqlserver")).thenReturn(new ConnectionConfig("sqlserver","sqlserver",1433,"BusinessDB","reader","private",true));
        when(sources.defaultSchema("sqlserver")).thenReturn("dbo");var id=UUID.randomUUID();
        var request=new QualityRequest("sqlserver",id,id,id);
        var wrong=new StorageJob(id,"COMPLETED",new CreateJobRequest("other","dbo","Invoice",null,null,null,null),Instant.now(),null,null,null,null,Map.of("catalog_table","hive.runs.invoice_test"));
        when(jobs.get(id)).thenReturn(wrong);
        var service=new QualityService(jobs,sources,PluginRegistry.discover(),trino,reader);
        assertTrue(assertThrows(QualityException.class,()->service.report(request)).invalid());verifyNoInteractions(trino,reader);
    }
    @Test void controllerValidatesInputAndSanitizesInfrastructureErrors() throws Exception{
        var service=mock(QualityService.class);var mvc=MockMvcBuilders.standaloneSetup(new QualityController(service)).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(post("/quality/reports").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isUnprocessableEntity());
        var id=UUID.randomUUID();String body="{\"source_id\":\"sqlserver\",\"customer_job_id\":\""+id+"\",\"invoice_job_id\":\""+id+"\",\"payment_job_id\":\""+id+"\"}";
        when(service.report(any())).thenThrow(new QualityException(false));
        mvc.perform(post("/quality/reports").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.detail").exists());
    }
}
