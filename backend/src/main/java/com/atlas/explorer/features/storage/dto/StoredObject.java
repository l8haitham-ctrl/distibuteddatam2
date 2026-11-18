package com.atlas.explorer.features.storage.dto;

public record StoredObject(String bucket, String key, long size, String contentType, String etag) {}
