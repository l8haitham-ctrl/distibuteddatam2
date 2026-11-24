package com.atlas.explorer.features.glossary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.*;

public record BusinessTerm(UUID id, String name, String definition,
        @JsonProperty("calculation_rule") String calculationRule, String owner, TermRequest.Status status,
        @JsonProperty("created_at") Instant createdAt, @JsonProperty("updated_at") Instant updatedAt,
        List<TermLink> links) {}
