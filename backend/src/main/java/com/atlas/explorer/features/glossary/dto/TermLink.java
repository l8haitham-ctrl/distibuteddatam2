package com.atlas.explorer.features.glossary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record TermLink(UUID id, @JsonProperty("term_id") UUID termId,
        @JsonProperty("source_id") String sourceId, @JsonProperty("schema_name") String schemaName,
        @JsonProperty("table_name") String tableName, @JsonProperty("column_name") String columnName,
        @JsonProperty("created_at") Instant createdAt) {}
