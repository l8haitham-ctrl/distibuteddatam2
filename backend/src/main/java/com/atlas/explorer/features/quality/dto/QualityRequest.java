package com.atlas.explorer.features.quality.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record QualityRequest(@NotBlank @JsonProperty("source_id") String sourceId,
    @NotNull @JsonProperty("customer_job_id") UUID customerJobId,
    @NotNull @JsonProperty("invoice_job_id") UUID invoiceJobId,
    @NotNull @JsonProperty("payment_job_id") UUID paymentJobId) {}
