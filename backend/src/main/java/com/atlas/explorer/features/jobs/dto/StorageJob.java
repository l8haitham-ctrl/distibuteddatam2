package com.atlas.explorer.features.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record StorageJob(UUID id, String status, CreateJobRequest request,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("finished_at") Instant finishedAt,
        @JsonProperty("submission_id") String submissionId,
        @JsonProperty("error_code") String errorCode,
        Map<String,Object> result) {
    public String prefix() { return request.sourceId()+"/"+request.schema()+"/"+request.table()+"/"+id; }
}
