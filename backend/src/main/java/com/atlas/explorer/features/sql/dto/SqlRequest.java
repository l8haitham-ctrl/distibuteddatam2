package com.atlas.explorer.features.sql.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
public record SqlRequest(@NotBlank @Size(max=20000) String sql,@JsonProperty("max_rows") @Min(1) @Max(1000) Integer maxRows) {
    public SqlRequest {maxRows=maxRows==null?1000:maxRows;}
}
