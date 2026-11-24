package com.atlas.explorer.features.glossary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record TermRequest(
        @NotBlank @Size(max=160) String name,
        @NotBlank @Size(max=8000) String definition,
        @JsonProperty("calculation_rule") @Size(max=8000) String calculationRule,
        @Size(max=160) String owner,
        @NotNull Status status) {
    public enum Status { DRAFT, VALIDATED }
    public TermRequest normalized() {
        return new TermRequest(name.strip(), definition.strip(),
                calculationRule==null?"":calculationRule.strip(), owner==null?"":owner.strip(), status);
    }
}
