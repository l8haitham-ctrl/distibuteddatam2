package com.atlas.explorer;

import com.atlas.explorer.features.jobs.api.JobController;
import com.atlas.explorer.features.jobs.config.JobProperties;
import com.atlas.explorer.features.jobs.connector.*;
import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.features.jobs.service.*;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import com.atlas.explorer.features.storage.service.StorageService;
import com.atlas.explorer.shared.domain.ExplorerException;
import com.atlas.explorer.shared.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobTests {
    private CreateJobRequest request() { return new CreateJobRequest("sqlserver",null,"Invoice",null,null,null,null); }
    private StorageJob job(String status,String submission,Instant start) { return new StorageJob(UUID.randomUUID(),status,request(),Instant.now(),start,null,submission,null,null); }
    private JobProperties properties() { return new JobProperties("jdbc:postgresql://metadata/atlas","atlas","private-password",1800,"http://spark-master:6066","spark://spark-master:7077"); }

    @Test void requestDefaultsAndInvalidInputNeverLaunchAnything() throws Exception {
        assertEquals(1000,request().fetchSize());assertEquals("dbo",request().schema());
        var service=mock(JobService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new JobController(service)).setControllerAdvice(new ApiExceptionHandler()).build();
        for(String body:List.of("{\"source_id\":\"sqlserver\",\"table\":\"Invoice;DROP TABLE Invoice\"}",
                "{\"source_id\":\"sqlserver\",\"table\":\"Invoice\",\"partition_by\":[\"invoice_date\"]}",
                "{\"source_id\":\"sqlserver\",\"table\":\"Invoice\",\"fetch_size\":0}",
                "{\"source_id\":\"sqlserver\",\"table\":\"Invoice\",\"format\":\"csv\"}")) {
            mvc.perform(post("/storage/jobs").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnprocessableEntity());
        }
        verifyNoInteractions(service);
    }
    @Test void unavailableSourceCreatesNoPersistedJob() {
        var repository=mock(JobRepository.class);var sources=mock(SourceService.class);var exploration=mock(ExplorationService.class);
        when(sources.get("sqlserver")).thenThrow(new ExplorerException(ExplorerException.Code.SOURCE_NOT_FOUND));
        assertThrows(ExplorerException.class,()->new JobService(repository,sources,exploration).create(request()));
        verifyNoInteractions(repository,exploration);
    }
    @Test void acceptedJobReturnsItsLocationWithoutWaitingForSpark() throws Exception {
        var service=mock(JobService.class);var job=job("QUEUED",null,null);when(service.create(any())).thenReturn(job);
        MockMvcBuilders.standaloneSetup(new JobController(service)).build().perform(post("/storage/jobs")
                .contentType(MediaType.APPLICATION_JSON).content("{\"source_id\":\"sqlserver\",\"table\":\"Invoice\"}"))
                .andExpect(status().isAccepted()).andExpect(header().string("Location","http://localhost/storage/jobs/"+job.id()))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }
    @Test void locationPreservesFrontendApiPrefix() throws Exception {
        var service=mock(JobService.class);var job=job("QUEUED",null,null);when(service.create(any())).thenReturn(job);
        MockMvcBuilders.standaloneSetup(new JobController(service)).addFilter(new org.springframework.web.filter.ForwardedHeaderFilter()).build()
                .perform(post("/storage/jobs").header("X-Forwarded-Prefix","/api").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source_id\":\"sqlserver\",\"table\":\"Invoice\"}"))
                .andExpect(status().isAccepted()).andExpect(header().string("Location","http://localhost/api/storage/jobs/"+job.id()));
    }
    @Test void sparkRestUsesFileUriAndDoesNotTransmitDatabaseCredentials() throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var captured=new java.util.concurrent.atomic.AtomicReference<String>();
        server.createContext("/v1/submissions/create",exchange -> {
            captured.set(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            byte[] response="{\"success\":true,\"submissionId\":\"driver-test-1\"}".getBytes();
            exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        try {
            var props=new JobProperties("jdbc:postgresql://metadata/atlas","atlas","private-password",1800,
                    "http://127.0.0.1:"+server.getAddress().getPort(),"spark://spark-master:7077");
            assertEquals("driver-test-1",new SparkJobEngine(props).submit(job("SUBMITTING",null,Instant.now())));
            var payload=new com.fasterxml.jackson.databind.ObjectMapper().readTree(captured.get());
            assertEquals("file:/opt/atlas/atlas-ingestion.jar",payload.get("appResource").asText());
            assertTrue(payload.get("environmentVariables").isEmpty());assertFalse(captured.get().contains("private-password"));
        } finally { server.stop(0); }
    }
    @Test void unfinishedJobHasNoPublishedResult() {
        var repository=mock(JobRepository.class);var job=job("RUNNING","driver-test-1",Instant.now());when(repository.get(job.id())).thenReturn(job);
        var service=new JobService(repository,mock(SourceService.class),mock(ExplorationService.class));
        assertEquals(ExplorerException.Code.JOB_NOT_COMPLETE,assertThrows(ExplorerException.class,()->service.result(job.id())).code());
    }
    @Test void ambiguousSubmissionFailsOnceWithoutAutomaticResubmission() {
        var repository=mock(JobRepository.class);var engine=mock(JobEngine.class);var storage=mock(StorageService.class);var job=job("SUBMITTING",null,Instant.now());
        when(repository.active()).thenReturn(List.of());when(repository.claim()).thenReturn(Optional.of(job));
        when(engine.submit(job)).thenThrow(new ExplorerException(ExplorerException.Code.JOB_ENGINE_UNAVAILABLE));
        new JobDispatcher(repository,engine,storage,properties()).tick();
        verify(repository).failed(job.id(),"SUBMISSION_UNCERTAIN");verify(repository,never()).running(any(),any());
    }
    @Test void transientStorageOutageDoesNotLoseRunningJob() {
        var repository=mock(JobRepository.class);var engine=mock(JobEngine.class);var storage=mock(StorageService.class);var job=job("RUNNING","driver-test-1",Instant.now());
        when(repository.active()).thenReturn(List.of(job));when(repository.claim()).thenReturn(Optional.empty());
        when(storage.open(any())).thenThrow(new ExplorerException(ExplorerException.Code.STORAGE_UNAVAILABLE));
        new JobDispatcher(repository,engine,storage,properties()).tick();
        verify(repository,never()).failed(any(),any());verify(repository,never()).completed(any(),any());
    }
    @Test void finishedDriverWithoutCommittedManifestFails() {
        var repository=mock(JobRepository.class);var engine=mock(JobEngine.class);var storage=mock(StorageService.class);var job=job("RUNNING","driver-test-1",Instant.now());
        when(repository.active()).thenReturn(List.of(job));when(repository.claim()).thenReturn(Optional.empty());
        when(storage.open(any())).thenThrow(new ExplorerException(ExplorerException.Code.OBJECT_NOT_FOUND));when(engine.state(job.submissionId())).thenReturn("FINISHED");
        new JobDispatcher(repository,engine,storage,properties()).tick();verify(repository).failed(job.id(),"RESULT_MISSING");
    }
    @Test void invalidManifestIsNeverPublished() {
        var repository=mock(JobRepository.class);var engine=mock(JobEngine.class);var storage=mock(StorageService.class);var job=job("RUNNING","driver-test-1",Instant.now());
        when(repository.active()).thenReturn(List.of(job));when(repository.claim()).thenReturn(Optional.empty());
        when(storage.open(any())).thenReturn(new ByteArrayInputStream("{\"job_id\":\"wrong-job\"}".getBytes()));
        new JobDispatcher(repository,engine,storage,properties()).tick();verify(repository).failed(job.id(),"RESULT_INVALID");
        verify(repository,never()).completed(any(),any());
    }
    @Test void timeoutIsEnforcedEvenDuringStorageOutage() {
        var repository=mock(JobRepository.class);var engine=mock(JobEngine.class);var storage=mock(StorageService.class);var job=job("RUNNING","driver-test-1",Instant.now().minusSeconds(2000));
        when(repository.active()).thenReturn(List.of(job));when(repository.claim()).thenReturn(Optional.empty());
        when(storage.open(any())).thenThrow(new ExplorerException(ExplorerException.Code.STORAGE_UNAVAILABLE));
        new JobDispatcher(repository,engine,storage,properties()).tick();verify(engine).kill(job.submissionId());verify(repository).failed(job.id(),"EXECUTION_TIMEOUT");
    }
    @Test void configurationDoesNotPrintCredentials() { assertFalse(properties().toString().contains("private-password")); }
}
