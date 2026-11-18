package com.atlas.explorer.features.jobs.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("jobs")
public record JobProperties(@NotBlank String jdbcUrl, @NotBlank String username, @NotBlank String password,
        @Min(60) int timeoutSeconds, @NotBlank String sparkRestUrl, @NotBlank String sparkMaster) {
    @Override public String toString() { return "JobProperties[credentials=REDACTED]"; }
}
