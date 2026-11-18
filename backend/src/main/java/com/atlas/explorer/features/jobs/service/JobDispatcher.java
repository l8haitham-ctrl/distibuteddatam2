package com.atlas.explorer.features.jobs.service;

import com.atlas.explorer.features.jobs.connector.*;
import com.atlas.explorer.features.jobs.config.JobProperties;
import com.atlas.explorer.features.jobs.dto.StorageJob;
import com.atlas.explorer.features.storage.service.StorageService;
import com.atlas.explorer.shared.domain.ExplorerException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;
import java.util.*;

public class JobDispatcher {
    private static final Logger LOG=LoggerFactory.getLogger(JobDispatcher.class);
    private final JobRepository repository;
    private final JobEngine engine;
    private final StorageService storage;
    private final int timeout;
    private final ObjectMapper json=new ObjectMapper();
    public JobDispatcher(JobRepository repository,JobEngine engine,StorageService storage,JobProperties properties) {
        this.repository=repository;this.engine=engine;this.storage=storage;this.timeout=properties.timeoutSeconds();
    }
    @Scheduled(fixedDelayString="${jobs.poll-delay-ms:2000}")
    public void tick() {
        try {
            for(var job:repository.active())monitor(job);
            repository.claim().ifPresent(job -> {
                try { repository.running(job.id(),engine.submit(job)); }
                catch(Exception exception) { repository.failed(job.id(),"SUBMISSION_UNCERTAIN"); }
            });
        } catch(Exception exception) {
            // Keep infrastructure failures retryable. Never log JDBC/Spark response bodies.
            LOG.warn("Job polling unavailable; retrying on the next poll.");
        }
    }
    private void monitor(StorageJob job) {
        if(job.status().equals("SUBMITTING")) {
            if(job.startedAt().plusSeconds(120).isBefore(Instant.now()))repository.failed(job.id(),"SUBMISSION_UNCERTAIN");
            return;
        }
        try {
            var result=manifest(job);
            if(result.isPresent()) { repository.completed(job.id(),result.get());return; }
            if(job.startedAt().plusSeconds(timeout).isBefore(Instant.now())) {
                try { engine.kill(job.submissionId()); } catch(Exception ignored) { }
                repository.failed(job.id(),"EXECUTION_TIMEOUT");return;
            }
            String state=engine.state(job.submissionId());
            if(Set.of("ERROR","FAILED","KILLED","LOST","UNKNOWN","FINISHED").contains(state)) {
                repository.failed(job.id(),state.equals("FINISHED")?"RESULT_MISSING":"SPARK_"+state);
            }
        } catch(ExplorerException exception) {
            // A temporarily unavailable Spark/MinIO service does not erase the active execution.
            if(exception.code()==ExplorerException.Code.JOB_RESULT_INVALID)repository.failed(job.id(),"RESULT_INVALID");
            else if(job.startedAt().plusSeconds(timeout).isBefore(Instant.now())) {
                try { engine.kill(job.submissionId()); } catch(Exception ignored) { }
                repository.failed(job.id(),"EXECUTION_TIMEOUT");
            }
        }
    }
    private Optional<Map<String,Object>> manifest(StorageJob job) {
        String key=job.prefix()+"/manifest.json";
        try {
            try(var input=storage.open(key)) {
                byte[] bytes=input.readNBytes(262145);
                if(bytes.length>262144)throw invalid();
                var result=json.readValue(bytes,new TypeReference<Map<String,Object>>(){});
                if(!job.id().toString().equals(result.get("job_id")) || !job.prefix().equals(result.get("prefix"))
                        || !"parquet".equals(result.get("format")) || !(result.get("row_count") instanceof Number rows) || rows.longValue()<0
                        || !(result.get("file_count") instanceof Number files) || files.longValue()<0
                        || !(result.get("storage_bytes") instanceof Number size) || size.longValue()<0)throw invalid();
                storage.stat(job.prefix()+"/data/_SUCCESS");
                var published=new LinkedHashMap<String,Object>(result);
                published.put("manifest_key",key);
                published.put("bucket",storage.stat(key).bucket());
                return Optional.of(published);
            } catch(java.io.IOException exception) { throw invalid(); }
        } catch(ExplorerException exception) {
            if(exception.code()==ExplorerException.Code.OBJECT_NOT_FOUND)return Optional.empty();
            throw exception;
        }
    }
    private ExplorerException invalid() { return new ExplorerException(ExplorerException.Code.JOB_RESULT_INVALID); }
}
