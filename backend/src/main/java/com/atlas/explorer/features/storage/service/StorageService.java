package com.atlas.explorer.features.storage.service;

import com.atlas.explorer.features.storage.connector.ObjectStorage;
import com.atlas.explorer.features.storage.dto.StoredObject;
import com.atlas.explorer.features.storage.dto.StorageStatus;
import com.atlas.explorer.shared.domain.ExplorerException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StorageService {
    private final ObjectStorage storage;
    private final String bucket;

    public StorageService(ObjectStorage storage, String bucket) { this.storage = storage; this.bucket = bucket; }

    public StorageStatus status() {
        if (!storage.bucketExists()) throw new ExplorerException(ExplorerException.Code.STORAGE_UNAVAILABLE);
        return new StorageStatus("ready", bucket);
    }

    /** Caller owns the input stream; known size prevents unbounded buffering. */
    public StoredObject put(String key, InputStream content, long size, String contentType) {
        validateKey(key);
        if (content == null || size < 0 || contentType == null || contentType.isBlank()
                || contentType.chars().anyMatch(Character::isISOControl)) throw invalid();
        return storage.put(key, content, size, contentType);
    }

    public StoredObject stat(String key) { validateKey(key); return storage.stat(key); }
    public InputStream open(String key) { validateKey(key); return storage.open(key); }
    public void delete(String key) { validateKey(key); storage.delete(key); }

    private static void validateKey(String key) {
        if (key == null || key.isBlank() || key.getBytes(StandardCharsets.UTF_8).length > 1024
                || key.contains("\\") || key.chars().anyMatch(Character::isISOControl)
                || Arrays.stream(key.split("/", -1)).anyMatch(part -> part.isEmpty() || part.equals(".") || part.equals(".."))) {
            throw invalid();
        }
    }

    private static ExplorerException invalid() { return new ExplorerException(ExplorerException.Code.STORAGE_INVALID); }
}
