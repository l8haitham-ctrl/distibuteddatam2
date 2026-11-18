package com.atlas.explorer.features.storage.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("storage")
public record StorageProperties(
        @NotBlank String endpoint,
        @NotBlank String region,
        @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,61}[a-z0-9]") String bucket,
        @NotBlank String accessKey,
        @NotBlank String secretKey) {
    @Override public String toString() { return "StorageProperties[credentials=REDACTED]"; }
}
