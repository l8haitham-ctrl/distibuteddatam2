package com.atlas.explorer.features.jobs.connector;

import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.shared.domain.ExplorerException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class JdbcJobRepository implements JobRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();
    public JdbcJobRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public StorageJob create(CreateJobRequest request) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO storage_job(id,request_json,status) VALUES (?,?,'QUEUED')",id,encode(request));
        return get(id);
    }
    public StorageJob get(UUID id) {
        var rows=jdbc.query("SELECT * FROM storage_job WHERE id=?",this::row,id);
        if(rows.isEmpty())throw new ExplorerException(ExplorerException.Code.JOB_NOT_FOUND);
        return rows.getFirst();
    }
    public List<StorageJob> list() { return jdbc.query("SELECT * FROM storage_job ORDER BY created_at DESC,id DESC LIMIT 100",this::row); }
    public List<StorageJob> active() { return jdbc.query("SELECT * FROM storage_job WHERE status IN ('SUBMITTING','RUNNING','REGISTERING') ORDER BY started_at",this::row); }
    public Optional<StorageJob> claim() {
        // Row locks select once; the partial unique index enforces global capacity under races.
        try { return jdbc.query("""
            WITH candidate AS (
                SELECT id FROM storage_job WHERE status='QUEUED'
                AND NOT EXISTS (SELECT 1 FROM storage_job WHERE status IN ('SUBMITTING','RUNNING','REGISTERING'))
                ORDER BY created_at,id FOR UPDATE SKIP LOCKED LIMIT 1
            )
            UPDATE storage_job SET status='SUBMITTING',started_at=now()
            WHERE id IN (SELECT id FROM candidate) RETURNING *
            """,this::row).stream().findFirst(); }
        catch(DuplicateKeyException exception) { return Optional.empty(); }
    }
    public void running(UUID id,String submissionId) {
        jdbc.update("UPDATE storage_job SET status='RUNNING',submission_id=? WHERE id=? AND status='SUBMITTING'",submissionId,id);
    }
    public void completed(UUID id,Map<String,Object> result) {
        jdbc.update("UPDATE storage_job SET status='COMPLETED',result_json=?,catalog_table=?,catalog_view=?,registration_ms=?,pending_result_json=NULL,finished_at=now() WHERE id=? AND status='REGISTERING'",
                encode(result),result.get("catalog_table"),result.get("current_view"),result.get("registration_ms"),id);
    }
    public void registering(UUID id,Map<String,Object> manifest) {
        jdbc.update("UPDATE storage_job SET status='REGISTERING',pending_result_json=? WHERE id=? AND status='RUNNING'",encode(manifest),id);
    }
    public void registrationError(UUID id) {
        jdbc.update("UPDATE storage_job SET error_code='CATALOG_REGISTRATION_FAILED' WHERE id=? AND status='REGISTERING'",id);
    }
    public Map<String,Object> pending(UUID id) {
        try {return json.readValue(jdbc.queryForObject("SELECT pending_result_json FROM storage_job WHERE id=?",String.class,id),new TypeReference<Map<String,Object>>(){});}
        catch(java.io.IOException exception){throw new IllegalStateException("Invalid pending manifest.");}
    }
    public void failed(UUID id,String code) {
        jdbc.update("UPDATE storage_job SET status='FAILED',error_code=?,finished_at=now() WHERE id=? AND status IN ('SUBMITTING','RUNNING','REGISTERING')",code,id);
    }
    private StorageJob row(ResultSet rs,int number) throws SQLException {
        try {
            return new StorageJob(rs.getObject("id",UUID.class),rs.getString("status"),json.readValue(rs.getString("request_json"),CreateJobRequest.class),
                    instant(rs,"created_at"),instant(rs,"started_at"),instant(rs,"finished_at"),rs.getString("submission_id"),rs.getString("error_code"),
                    rs.getString("result_json")==null?null:json.readValue(rs.getString("result_json"),new TypeReference<Map<String,Object>>(){}));
        } catch (java.io.IOException exception) { throw new IllegalStateException("Invalid persisted job metadata."); }
    }
    private Instant instant(ResultSet rs,String column) throws SQLException { var value=rs.getTimestamp(column);return value==null?null:value.toInstant(); }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch(java.io.IOException exception) { throw new IllegalStateException("Cannot encode job metadata."); }
    }
}
