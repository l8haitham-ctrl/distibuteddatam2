package com.atlas.explorer.features.nativesources.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record NativeConnection(
    @NotBlank @Pattern(regexp="redis|minio") String engine,
    @NotBlank @Size(max=255) @Pattern(regexp="[a-zA-Z0-9_.:-]+") String host,
    @NotNull @Min(1) @Max(65535) Integer port,
    @NotBlank @Size(max=128) String namespace,
    @NotBlank @Size(max=128) String username,
    @NotBlank @Size(max=128) @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) String password,
    Boolean tls,
    @Size(max=512) String prefix) {
    public NativeConnection { tls=tls==null?true:tls; prefix=prefix==null?"":prefix; }
    @Override public String toString(){return "NativeConnection[engine="+engine+", credentials=REDACTED]";}
}
