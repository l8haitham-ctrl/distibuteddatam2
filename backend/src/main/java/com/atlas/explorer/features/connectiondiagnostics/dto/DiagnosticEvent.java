package com.atlas.explorer.features.connectiondiagnostics.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
public record DiagnosticEvent(String step,String state,String message,@JsonProperty("duration_ms") long durationMs) {}
