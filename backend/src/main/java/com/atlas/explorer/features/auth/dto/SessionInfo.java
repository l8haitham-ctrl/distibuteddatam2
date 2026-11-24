package com.atlas.explorer.features.auth.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
public record SessionInfo(boolean authenticated,String username,@JsonProperty("csrf_token") String csrfToken) {}
