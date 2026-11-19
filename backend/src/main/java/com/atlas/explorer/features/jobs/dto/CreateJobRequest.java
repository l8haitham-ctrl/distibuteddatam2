package com.atlas.explorer.features.jobs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateJobRequest(
        @JsonProperty("source_id") @NotBlank @Size(max=64) String sourceId,
        @Pattern(regexp="[A-Za-z_][A-Za-z0-9_]{0,127}") String schema,
        @NotBlank @Pattern(regexp="[A-Za-z_][A-Za-z0-9_]{0,127}") String table,
        @Pattern(regexp="parquet|csv") String format,
        @JsonProperty("partition_by") @Size(max=8) List<@NotBlank @Pattern(regexp="[A-Za-z_][A-Za-z0-9_]{0,127}") String> partitionBy,
        @JsonProperty("fetch_size") @Min(1) @Max(100000) Integer fetchSize,
        @Pattern(regexp="snappy|zstd|gzip|uncompressed") String compression,
        @JsonProperty("partition_granularity") @Pattern(regexp="month|year|day") String partitionGranularity,
        @JsonProperty("num_partitions") @Min(1) @Max(16) Integer numPartitions) {
    public CreateJobRequest(String sourceId,String schema,String table,String format,List<String> partitionBy,Integer fetchSize,String compression) {
        this(sourceId,schema,table,format,partitionBy,fetchSize,compression,null,null);
    }
    public CreateJobRequest {
        schema = schema == null ? "dbo" : schema;
        format = format == null ? "parquet" : format;
        partitionBy = partitionBy == null ? List.of() : List.copyOf(partitionBy);
        fetchSize = fetchSize == null ? 1000 : fetchSize;
        compression = compression == null ? (format.equals("csv") ? "gzip" : "snappy") : compression;
        partitionGranularity = partitionGranularity == null ? "month" : partitionGranularity;
        numPartitions = numPartitions == null ? 4 : numPartitions;
    }
}
