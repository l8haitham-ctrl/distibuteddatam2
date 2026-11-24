package com.atlas.explorer.features.glossary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record LinkRequest(
        @JsonProperty("source_id") @NotBlank @Size(max=128) String sourceId,
        @JsonProperty("schema_name") @NotBlank @Size(max=128) String schemaName,
        @JsonProperty("table_name") @NotBlank @Size(max=128) String tableName,
        @JsonProperty("column_name") @Pattern(regexp="(?s).*\\S.*") @Size(max=128) String columnName) {}
