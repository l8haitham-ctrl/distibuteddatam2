package com.atlas.explorer;

import com.atlas.explorer.features.jobs.config.JobProperties;
import com.atlas.explorer.features.jobs.connector.*;
import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.features.jobs.service.JobDispatcher;
import com.atlas.explorer.features.storage.service.StorageService;
import io.minio.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_JOB_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobIntegrationTests {
    @Autowired JobRepository repository;
    @Autowired com.atlas.explorer.features.catalog.service.CatalogService catalog;
    @Autowired JobEngine engine;
    @Autowired StorageService storage;
    @Autowired JobProperties properties;
    @Autowired DataSource dataSource;
    @Autowired MinioClient minio;
    @Autowired org.springframework.core.env.Environment environment;
    @Autowired com.atlas.explorer.features.catalog.connector.TrinoConnections trino;
    @Autowired com.atlas.explorer.features.sql.service.SqlService sql;
    @Autowired com.atlas.explorer.features.connection.service.SourceService sources;
    @LocalServerPort int port;
    private JdbcTemplate jdbc;
    @BeforeEach void isolate() throws Exception {
        assertTrue(properties.jdbcUrl().endsWith("/atlas_test"),"Job tests require their isolated database.");
        cleanupObjects();
        jdbc=new JdbcTemplate(dataSource);jdbc.update("DELETE FROM storage_job");
    }
    @AfterEach void cleanupObjects() throws Exception {
        for(var job:repository.list()) {
            if(job.submissionId()!=null && job.status().equals("RUNNING")) {
                try { engine.kill(job.submissionId()); } catch(Exception ignored) { }
            }
            // Test schemas never replace application views; only remove this test's metadata.
            String run="hive.runs_test."+job.request().table().toLowerCase(Locale.ROOT)+"_"+job.id().toString().substring(0,8);
            String view="hive.sqlserver_dbo_test."+job.request().table().toLowerCase(Locale.ROOT);
            try(var connection=trino.open(true);var statement=connection.createStatement()) {
                statement.setQueryTimeout(60);
                boolean ownView=false;
                try(var ddl=statement.executeQuery("SHOW CREATE VIEW "+view)) {ownView=ddl.next()&&ddl.getString(1).contains(run);}
                catch(java.sql.SQLException ignored){}
                if(ownView)statement.execute("DROP VIEW "+view);
                statement.execute("DROP TABLE IF EXISTS "+run);
            }
            for(var result:minio.listObjects(ListObjectsArgs.builder().bucket(storage.status().bucket()).prefix(job.prefix()+"/").recursive(true).build())) {
                minio.removeObject(RemoveObjectArgs.builder().bucket(storage.status().bucket()).object(result.get().objectName()).build());
            }
        }
    }
    private CreateJobRequest request(String table) { return new CreateJobRequest("sqlserver","dbo",table,null,null,null,null); }
    private HttpResponse<String> call(String method,String path,String body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if(body==null)builder.method(method,HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body));
        return com.atlas.explorer.support.AuthenticatedHttp.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void apiValidatesBeforeQueuingAndExposesPendingStatus() throws Exception {
        assertEquals(422,call("POST","/storage/jobs","{\"source_id\":\"sqlserver\",\"table\":\"Invoice\",\"fetch_size\":0}").statusCode());
        assertEquals(404,call("POST","/storage/jobs","{\"source_id\":\"unknown\",\"table\":\"Invoice\"}").statusCode());
        assertEquals(404,call("POST","/storage/jobs","{\"source_id\":\"sqlserver\",\"table\":\"MissingTable\"}").statusCode());
        assertTrue(repository.list().isEmpty());
        var response=call("POST","/storage/jobs","{\"source_id\":\"sqlserver\",\"table\":\"Invoice\"}");
        assertEquals(202,response.statusCode(),response.body());
        String location=URI.create(response.headers().firstValue("Location").orElseThrow()).getPath();
        assertEquals(200,call("GET",location,null).statusCode());
        assertEquals(409,call("GET",location+"/result",null).statusCode());
        assertEquals(404,call("GET","/storage/jobs/"+UUID.randomUUID(),null).statusCode());
        assertEquals(400,call("GET","/storage/jobs/not-a-uuid",null).statusCode());
        assertFalse(response.body().contains("password"));
    }
    @Test void partitionCandidatesUseRealCardinalityAndNeverQueue() throws Exception {
        var response=call("GET","/storage/jobs/partition-candidates?source_id=sqlserver&table=Invoice",null);
        assertEquals(200,response.statusCode(),response.body());
        var values=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
        var eligible=new HashSet<String>();
        values.forEach(value->{if(value.get("eligible").asBoolean())eligible.add(value.get("name").asText());});
        assertTrue(eligible.containsAll(List.of("invoice_date","status")));assertFalse(eligible.contains("invoice_id"));assertFalse(eligible.contains("amount"));
        assertEquals(422,call("GET","/storage/jobs/partition-candidates?source_id=sqlserver&table=Invoice&partition_granularity=hour",null).statusCode());
        assertTrue(repository.list().isEmpty());
    }
    @Test void realSourcePartitionChecksRejectUnsafeRequestsWithoutQueuing() throws Exception {
        for(String column:List.of("missing","invoice_id","amount")) {
            var response=call("POST","/storage/jobs","{\"source_id\":\"sqlserver\",\"table\":\"Invoice\",\"partition_by\":[\""+column+"\"]}");
            assertEquals(422,response.statusCode(),response.body());assertTrue(response.body().contains("partition_by"));
        }
        assertEquals(422,call("POST","/storage/jobs","{\"source_id\":\"sqlserver\",\"table\":\"Invoice\",\"format\":\"csv\",\"compression\":\"zstd\"}").statusCode());
        assertTrue(repository.list().isEmpty());
        var response=call("POST","/storage/jobs","{\"source_id\":\"sqlserver\",\"table\":\"Invoice\",\"format\":\"parquet\",\"partition_by\":[\"invoice_date\"]}");
        assertEquals(202,response.statusCode(),response.body());
    }
    @ParameterizedTest @CsvSource({"parquet,zstd","csv,gzip","csv,uncompressed"})
    void realSparkWritesTypedMonthlyPartitions(String format,String compression) throws Exception {
        var request=new CreateJobRequest("sqlserver","dbo","Invoice",format,List.of("invoice_date"),257,compression,"month",4);
        var job=run(request);var result=job.result();
        assertEquals(5000,((Number)result.get("row_count")).longValue());
        assertEquals(4,((Number)result.get("effective_num_partitions")).intValue());
        assertEquals("invoice_id",result.get("jdbc_partition_column"));
        @SuppressWarnings("unchecked") var partitions=(Map<String,Number>)result.get("partition_file_counts");
        assertEquals(24,partitions.size());
        assertTrue(partitions.containsKey("invoice_date_year=2025/invoice_date_month=3"));
        assertEquals(((Number)result.get("file_count")).longValue(),partitions.values().stream().mapToLong(Number::longValue).sum());
        assertTrue(result.get("partition_columns").toString().contains("integer"));
        for(String timing:List.of("extract_write_ms","verify_ms","spark_duration_ms"))assertTrue(((Number)result.get(timing)).longValue()>0);
        long bytes=0,rows=0;var sum=java.math.BigDecimal.ZERO;
        for(var entry:minio.listObjects(ListObjectsArgs.builder().bucket(storage.status().bucket()).prefix(job.prefix()+"/data/").recursive(true).build())) {
            var object=entry.get();if(object.objectName().endsWith("/_SUCCESS"))continue;
            assertTrue(object.objectName().contains("invoice_date_year="),object.objectName());
            bytes+=object.size();
            if(format.equals("csv"))try(var raw=storage.open(object.objectName());
                    var input=compression.equals("gzip")?new java.util.zip.GZIPInputStream(raw):raw;
                    var reader=new java.io.BufferedReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8))) {
                String line;while((line=reader.readLine())!=null) {
                    String[] fields=line.split(",",-1);assertEquals(6,fields.length,line);
                    assertTrue(fields[0].matches("[0-9]+"),line);
                    assertTrue(fields[2].matches("202[56]-[0-9]{2}-[0-9]{2}"),line);
                    assertTrue(fields[3].matches("[0-9]+\\.[0-9]{2}"),line);
                    assertTrue(fields[5].matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}"),line);
                    sum=sum.add(new java.math.BigDecimal(fields[3]));rows++;
                }
            }
        }
        assertEquals(((Number)result.get("storage_bytes")).longValue(),bytes);
        if(format.equals("csv")) { assertEquals(5000,rows);assertTrue(sum.signum()>0);assertNotNull(result.get("csv_options")); }
        var response=call("GET","/storage/jobs/"+job.id()+"/result",null);
        assertEquals(200,response.statusCode());assertTrue(response.body().contains("extract_write_ms"));
    }
    @Test void catalogMatchesSourceAggregatesAndLatestView() throws Exception {
        run(request("Customer"));run(request("Payment"));
        var invoice=run(new CreateJobRequest("sqlserver","dbo","Invoice","parquet",List.of("invoice_date"),1000,"snappy"));
        String view=invoice.result().get("current_view").toString();
        var counts=sql.query(new com.atlas.explorer.features.sql.dto.SqlRequest("SELECT count(*),sum(amount) FROM "+view,1000));
        @SuppressWarnings("unchecked") var row=((List<List<Object>>)counts.get("rows")).getFirst();
        try(var connection=com.atlas.explorer.features.connection.connector.SqlServerConnections.open(sources.get("sqlserver"))) {
            var expected=com.atlas.explorer.features.connection.connector.SqlServerConnections.query(connection,"SELECT COUNT_BIG(*) AS n,SUM(amount) AS total FROM dbo.Invoice").getFirst();
            assertEquals(((Number)expected.get("n")).longValue(),((Number)row.getFirst()).longValue());
            assertEquals(0,new java.math.BigDecimal(expected.get("total").toString()).compareTo(new java.math.BigDecimal(row.get(1).toString())));
            String sourceQuery="SELECT c.city,SUM(p.amount) AS total FROM dbo.Customer c JOIN dbo.Invoice i ON i.customer_id=c.customer_id JOIN dbo.Payment p ON p.invoice_id=i.invoice_id GROUP BY c.city ORDER BY c.city";
            var expectedJoin=com.atlas.explorer.features.connection.connector.SqlServerConnections.query(connection,sourceQuery);
            String query=sourceQuery.replace("dbo.Customer","hive.sqlserver_dbo_test.customer").replace("dbo.Invoice",view).replace("dbo.Payment","hive.sqlserver_dbo_test.payment");
            @SuppressWarnings("unchecked") var rows=(List<List<Object>>)sql.query(new com.atlas.explorer.features.sql.dto.SqlRequest(query,1000)).get("rows");
            assertEquals(expectedJoin.size(),rows.size());
            for(int i=0;i<rows.size();i++) {
                assertEquals(expectedJoin.get(i).get("city"),rows.get(i).get(0));
                assertEquals(0,new java.math.BigDecimal(expectedJoin.get(i).get("total").toString()).compareTo(new java.math.BigDecimal(rows.get(i).get(1).toString())));
            }
        }
        var newer=run(request("Invoice"));
        try(var connection=trino.open(true);var statement=connection.createStatement()) {
            catalog.register(invoice,invoice.result()); // Idempotent old registration must not rewind the latest view.
            try(var ddl=statement.executeQuery("SHOW CREATE VIEW "+view)) {
                assertTrue(ddl.next());assertTrue(ddl.getString(1).contains(newer.result().get("catalog_table").toString()));
            }
        }
        assertTrue(catalog.tables().stream().anyMatch(t->t.get("job_id").equals(newer.id().toString())));
        assertTrue(((Number)newer.result().get("registration_ms")).longValue()>=0);
    }
    @Test void catalogStorageCredentialsCannotWriteOrDeleteData() throws Exception {
        String key="sqlserver/test-policy/"+UUID.randomUUID();byte[] content="read-only-check".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String readerKey=environment.getRequiredProperty("TRINO_S3_ACCESS_KEY");
        assertFalse(readerKey.equals(environment.getRequiredProperty("STORAGE_ACCESS_KEY")));
        try(var reader=MinioClient.builder().endpoint(environment.getRequiredProperty("STORAGE_ENDPOINT"))
                .credentials(readerKey,environment.getRequiredProperty("TRINO_S3_SECRET_KEY")).build()) {
            String bucket=storage.status().bucket();storage.put(key,new java.io.ByteArrayInputStream(content),content.length,"text/plain");
            try {
                try(var input=reader.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {assertArrayEquals(content,input.readAllBytes());}
                var put=assertThrows(io.minio.errors.ErrorResponseException.class,()->reader.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                        .stream(new java.io.ByteArrayInputStream(content),(long)content.length,-1L).build()));
                assertEquals("AccessDenied",put.errorResponse().code());
                var delete=assertThrows(io.minio.errors.ErrorResponseException.class,()->reader.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build()));
                assertEquals("AccessDenied",delete.errorResponse().code());
            } finally {storage.delete(key);}
        }
    }
    @Test void failedRegistrationRemovesItsExternalTableAndKeepsFiles() throws Exception {
        var job=repository.create(request("Invoice"));repository.claim();repository.running(job.id(),"driver-test-1");
        storage.put(job.prefix()+"/data/_SUCCESS",new java.io.ByteArrayInputStream(new byte[0]),0,"application/octet-stream");
        var manifest=new HashMap<String,Object>();manifest.put("job_id",job.id().toString());manifest.put("bucket",storage.status().bucket());
        manifest.put("prefix",job.prefix());manifest.put("source_id","sqlserver");manifest.put("schema_name","dbo");manifest.put("table_name","Invoice");
        manifest.put("format","parquet");manifest.put("row_count",1);manifest.put("schema_json","{\"fields\":[{\"name\":\"invoice_id\",\"type\":\"integer\"}]}");
        repository.registering(job.id(),manifest);
        new JobDispatcher(repository,engine,storage,properties,catalog).tick();
        var failed=repository.get(job.id());assertEquals("FAILED",failed.status());assertEquals("CATALOG_REGISTRATION_FAILED",failed.errorCode());
        assertNotNull(storage.stat(job.prefix()+"/data/_SUCCESS"));
        try(var connection=trino.open(false);var statement=connection.createStatement()) {
            assertThrows(java.sql.SQLException.class,()->statement.executeQuery("SELECT * FROM hive.runs_test.invoice_"+job.id().toString().substring(0,8)));
        }
    }
    @Test void trinoEnforcesReadOnlyAccessAndApiCapsRows() throws Exception {
        // Direct JDBC bypasses the API validator; Trino itself must deny writes/procedures/impersonation.
        try(var connection=trino.open(false);var statement=connection.createStatement()) {
            assertThrows(java.sql.SQLException.class,()->statement.execute("CREATE SCHEMA hive.reader_should_not_create"));
            assertThrows(java.sql.SQLException.class,()->statement.execute("CALL hive.system.sync_partition_metadata('runs_test','missing','FULL')"));
        }
        var result=sql.query(new com.atlas.explorer.features.sql.dto.SqlRequest("SELECT n FROM UNNEST(sequence(1,20)) AS t(n)",5));
        assertEquals(5,result.get("row_count"));assertEquals(true,result.get("truncated"));assertNotNull(result.get("query_id"));
        var failed=repository.create(request("Invoice"));repository.claim();repository.running(failed.id(),"driver-test-1");repository.failed(failed.id(),"TEST_FAILURE");
        assertTrue(catalog.tables().stream().noneMatch(t->t.get("job_id").equals(failed.id().toString())));
        try(var connection=trino.open(false);var statement=connection.createStatement()) {
            assertThrows(java.sql.SQLException.class,()->statement.executeQuery("SELECT * FROM hive.runs_test.invoice_"+failed.id().toString().substring(0,8)));
        }
        assertEquals(422,call("POST","/sql/query","{\"sql\":\"DROP TABLE invoice\"}").statusCode());
        assertEquals(422,call("POST","/sql/query","{\"sql\":\"SELECT 1\",\"max_rows\":1001}").statusCode());
        assertEquals(200,call("GET","/catalog/tables",null).statusCode());
    }
    private StorageJob run(CreateJobRequest request) throws Exception {
        var job=repository.create(request);var dispatcher=new JobDispatcher(repository,engine,storage,properties,catalog);
        dispatcher.tick();Instant deadline=Instant.now().plusSeconds(600);
        while(Instant.now().isBefore(deadline)) {
            dispatcher.tick();job=repository.get(job.id());
            if(Set.of("COMPLETED","FAILED").contains(job.status()))break;
            Thread.sleep(2000);
        }
        assertEquals("COMPLETED",job.status(),"Spark execution: "+job.errorCode());
        System.out.println("Verified SQL run: "+request.table()+" / "+request.format()+" / rows="+job.result().get("row_count")+" / registration_ms="+job.result().get("registration_ms"));return job;
    }
    @Test void statePersistsAcrossRepositoryRecreationAndTerminalStatesCannotRegress() {
        var job=repository.create(request("Invoice"));repository.claim().orElseThrow();repository.running(job.id(),"driver-test-1");
        var reopened=new JdbcJobRepository(new JdbcTemplate(dataSource));
        assertEquals("RUNNING",reopened.get(job.id()).status());assertEquals("driver-test-1",reopened.get(job.id()).submissionId());
        reopened.failed(job.id(),"TEST_FAILURE");reopened.running(job.id(),"driver-test-2");
        assertEquals("FAILED",repository.get(job.id()).status());assertNotNull(repository.get(job.id()).finishedAt());
    }
    @Test void concurrentDispatchClaimsOnlyOneJob() throws Exception {
        for(int i=0;i<5;i++)repository.create(request("Invoice"));
        try(var pool=Executors.newFixedThreadPool(8)) {
            var start=new CountDownLatch(1);var futures=new ArrayList<Future<Optional<StorageJob>>>();
            for(int i=0;i<8;i++)futures.add(pool.submit(()->{start.await();return repository.claim();}));
            start.countDown();int claimed=0;for(var future:futures)if(future.get().isPresent())claimed++;
            assertEquals(1,claimed);assertEquals(1,repository.active().size());
        }
    }
    @Test void metadataAccountIsNotAnAdministrator() {
        assertFalse(Boolean.TRUE.equals(jdbc.queryForObject("SELECT rolsuper FROM pg_roles WHERE rolname=current_user",Boolean.class)));
        assertFalse(Boolean.TRUE.equals(jdbc.queryForObject("SELECT rolcreatedb FROM pg_roles WHERE rolname=current_user",Boolean.class)));
    }
    @Test void realSparkWritesReadableParquetAndManifest() throws Exception {
        var job=repository.create(request("Invoice"));var dispatcher=new JobDispatcher(repository,engine,storage,properties,catalog);
        dispatcher.tick();Instant deadline=Instant.now().plusSeconds(600);
        while(Instant.now().isBefore(deadline)) {
            dispatcher.tick();job=repository.get(job.id());
            if(Set.of("COMPLETED","FAILED").contains(job.status()))break;
            Thread.sleep(2000);
        }
        assertEquals("COMPLETED",job.status(),"Spark execution: "+job.errorCode());
        assertNotNull(job.submissionId());assertEquals(5000,((Number)job.result().get("row_count")).longValue());
        assertTrue(((Number)job.result().get("file_count")).longValue()>0);
        assertTrue(((Number)job.result().get("storage_bytes")).longValue()>0);
        String schema=(String)job.result().get("schema_json");
        assertTrue(schema.contains("decimal(12,2)"),schema);assertTrue(schema.contains("invoice_date"));
        assertNotNull(storage.stat(job.prefix()+"/manifest.json"));
        for(var result:minio.listObjects(ListObjectsArgs.builder().bucket(storage.status().bucket()).prefix(job.prefix()+"/data/").recursive(true).build())) {
            var object=result.get();if(object.objectName().endsWith(".parquet")) {
                try(var input=storage.open(object.objectName())) { assertArrayEquals(new byte[]{'P','A','R','1'},input.readNBytes(4)); }
            }
        }
        assertEquals(200,call("GET","/storage/jobs/"+job.id()+"/result",null).statusCode());
    }
}
