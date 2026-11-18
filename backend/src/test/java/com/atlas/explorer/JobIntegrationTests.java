package com.atlas.explorer;

import com.atlas.explorer.features.jobs.config.JobProperties;
import com.atlas.explorer.features.jobs.connector.*;
import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.features.jobs.service.JobDispatcher;
import com.atlas.explorer.features.storage.service.StorageService;
import io.minio.*;
import org.junit.jupiter.api.*;
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
    @Autowired JobEngine engine;
    @Autowired StorageService storage;
    @Autowired JobProperties properties;
    @Autowired DataSource dataSource;
    @Autowired MinioClient minio;
    @LocalServerPort int port;
    private JdbcTemplate jdbc;
    @BeforeEach void isolate() {
        assertTrue(properties.jdbcUrl().endsWith("/atlas_test"),"Job tests require their isolated database.");
        jdbc=new JdbcTemplate(dataSource);jdbc.update("DELETE FROM storage_job");
    }
    @AfterEach void cleanupObjects() throws Exception {
        for(var job:repository.list()) {
            if(job.submissionId()!=null && job.status().equals("RUNNING")) {
                try { engine.kill(job.submissionId()); } catch(Exception ignored) { }
            }
            for(var result:minio.listObjects(ListObjectsArgs.builder().bucket(storage.status().bucket()).prefix(job.prefix()+"/").recursive(true).build())) {
                minio.removeObject(RemoveObjectArgs.builder().bucket(storage.status().bucket()).object(result.get().objectName()).build());
            }
        }
    }
    private CreateJobRequest request(String table) { return new CreateJobRequest("sqlserver",null,table,null,null,null,null); }
    private HttpResponse<String> call(String method,String path,String body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if(body==null)builder.method(method,HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(builder.build(),HttpResponse.BodyHandlers.ofString());
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
        var job=repository.create(request("Invoice"));var dispatcher=new JobDispatcher(repository,engine,storage,properties);
        dispatcher.tick();Instant deadline=Instant.now().plusSeconds(240);
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
