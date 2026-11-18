package com.atlas.explorer.features.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateJobRequest(
        @JsonProperty("source_id") @NotBlank @Size(max=64) String sourceId,
        @Pattern(regexp="[A-Za-z_][A-Za-z0-9_]{0,127}") String schema,
        @NotBlank @Pattern(regexp="[A-Za-z_][A-Za-z0-9_]{0,127}") String table,
        @Pattern(regexp="parquet") String format,
        @JsonProperty("partition_by") @Size(max=0) List<String> partitionBy,
        @JsonProperty("fetch_size") @Min(1) @Max(100000) Integer fetchSize,
        @Pattern(regexp="snappy|uncompressed") String compression) {
    public CreateJobRequest {
        schema = schema == null ? "dbo" : schema;
        format = format == null ? "parquet" : format;
        partitionBy = partitionBy == null ? List.of() : List.copyOf(partitionBy);
        fetchSize = fetchSize == null ? 1000 : fetchSize;
        compression = compression == null ? "snappy" : compression;
    }
}
