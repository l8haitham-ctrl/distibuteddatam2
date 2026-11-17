package com.atlas.explorer.features.connection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record ConnectionConfig(
        @NotBlank @Pattern(regexp = "sqlserver") String engine,
        @NotBlank @Size(max = 255) @Pattern(regexp = "[a-zA-Z0-9_.:-]+") String host,
        @NotNull @Min(1) @Max(65535) Integer port,
        @NotBlank @Size(max = 128) String database,
        @NotBlank @Size(max = 128) String username,
        @NotBlank @Size(max = 128) @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password,
        @JsonProperty("trust_server_certificate") Boolean trustServerCertificate) {
    @Override public String toString() {
        return "ConnectionConfig[engine=" + engine + ", credentials=REDACTED]";
    }
}
