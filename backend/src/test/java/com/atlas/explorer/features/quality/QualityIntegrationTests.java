package com.atlas.explorer.features.quality;
import com.atlas.explorer.features.quality.connector.*;
import com.atlas.explorer.features.catalog.connector.TrinoConnections;
import com.atlas.explorer.features.catalog.config.CatalogProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="RUN_QUALITY_TESTS",matches="true")
class QualityIntegrationTests {
    @Test
    @EnabledIfEnvironmentVariable(named="RUN_PG_TESTS",matches="true")
    void businessRulesAlsoReadRealPostgresqlSource() throws Exception {
        var plugins=com.atlas.explorer.features.connectors.connector.PluginRegistry.discover();
        var source=com.atlas.explorer.features.connectors.connector.SourceDefinitions.get("postgresql",System::getenv,plugins);
        var plugin=plugins.get("postgresql");
        try(var connection=plugin.open(source.settings())){
            var values=new JdbcQualityReader().read(connection,QualityQueries.build(n->plugin.qualified("public",n.toLowerCase(Locale.ROOT))));
            assertEquals("5000",values.get("invoice").get("row_count"));
            assertTrue(com.atlas.explorer.features.quality.service.QualityRules.evaluate(values,values,values).stream().allMatch(c->c.get("status").equals("passed")));
        }
    }
    @Test void sqlRulesDetectRealAnomaliesWithoutWritingData() throws Exception {
        var connections=new TrinoConnections(new CatalogProperties(System.getenv("TRINO_JDBC_URL"),System.getenv("TRINO_REGISTRAR_PASSWORD"),System.getenv("TRINO_READER_PASSWORD"),""));
        var fixture=Map.of(
            "Customer","(SELECT * FROM (VALUES (1,'a','b','a@example.test','Rabat',TIMESTAMP '2025-01-01 00:00:00',TIMESTAMP '2025-01-02 00:00:00'),(1,'a','b','a@example.test','Rabat',TIMESTAMP '2025-01-01 00:00:00',TIMESTAMP '2025-01-02 00:00:00')) AS t(customer_id,first_name,last_name,email,city,created_at,updated_at))",
            "Invoice","(SELECT * FROM (VALUES (1,1,DATE '2025-01-01',DECIMAL '100.00','paid',TIMESTAMP '2025-01-02 00:00:00'),(2,999,DATE '2025-01-01',DECIMAL '-20.00','unpaid',TIMESTAMP '2025-01-02 00:00:00')) AS t(invoice_id,customer_id,invoice_date,amount,status,updated_at))",
            "Payment","(SELECT * FROM (VALUES (1,1,DATE '2024-12-31',DECIMAL '50.00','card',TIMESTAMP '2025-01-02 00:00:00'),(2,999,DATE '2025-01-01',DECIMAL '-10.00','cash',TIMESTAMP '2025-01-02 00:00:00')) AS t(payment_id,invoice_id,payment_date,amount,payment_method,updated_at))");
        try(var connection=connections.open(false)){
            var result=new JdbcQualityReader().read(connection,QualityQueries.build(fixture::get));
            assertEquals("2",result.get("customer").get("row_count"));assertEquals("1",result.get("customer").get("distinct_ids"));assertEquals("1",result.get("customer").get("duplicate_emails"));
            for(String rule:List.of("orphan_invoices","orphan_payments","payment_dates","invoice_balances"))assertEquals("1",result.get(rule).get("violations"),rule);
            assertEquals("1",result.get("invoice").get("invalid_amounts"));assertEquals("1",result.get("payment").get("invalid_amounts"));
        }
    }
    @Test void deployedApiReconcilesThreeImmutableExtractionsAndRejectsWrongTables() throws Exception {
        var mapper=new ObjectMapper();var http=HttpClient.newHttpClient();
        var body=new LinkedHashMap<String,String>();body.put("source_id","sqlserver");
        body.put("customer_job_id",System.getenv("QUALITY_CUSTOMER_JOB"));body.put("invoice_job_id",System.getenv("QUALITY_INVOICE_JOB"));body.put("payment_job_id",System.getenv("QUALITY_PAYMENT_JOB"));
        var request=HttpRequest.newBuilder(URI.create("http://backend:8000/quality/reports")).timeout(java.time.Duration.ofSeconds(120)).header("Content-Type","application/json");
        var response=com.atlas.explorer.support.AuthenticatedHttp.send(request.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());var json=mapper.readTree(response.body());assertEquals("passed",json.get("status").asText());assertTrue(json.get("checks").size()>50);
        body.put("customer_job_id",body.get("invoice_job_id"));
        var invalid=com.atlas.explorer.support.AuthenticatedHttp.send(request.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());assertEquals(422,invalid.statusCode());
    }
}
