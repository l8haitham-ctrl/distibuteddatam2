package com.atlas.explorer.features.storage.connector;

import com.atlas.explorer.features.storage.dto.StoredObject;
import com.atlas.explorer.shared.domain.ExplorerException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;

public class MinioObjectStorage implements ObjectStorage {
    // Fixed multipart size bounds SDK buffering independently of file size.
    private static final long PART_SIZE = 5L * 1024 * 1024;
    private final MinioClient client;
    private final String bucket;

    public MinioObjectStorage(MinioClient client, String bucket) { this.client = client; this.bucket = bucket; }

    public boolean bucketExists() {
        try { return client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()); }
        catch (Exception exception) { throw sanitized(exception); }
    }

    public StoredObject put(String key, InputStream content, long size, String contentType) {
        try {
            var response = client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(content, size, PART_SIZE).contentType(contentType).build());
            return new StoredObject(bucket, key, size, contentType, response.etag());
        } catch (Exception exception) { throw sanitized(exception); }
    }

    public StoredObject stat(String key) {
        try {
            var response = client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return new StoredObject(bucket, key, response.size(), response.contentType(), response.etag());
        } catch (Exception exception) { throw sanitized(exception); }
    }

    public InputStream open(String key) {
        try { return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception exception) { throw sanitized(exception); }
    }

    public void delete(String key) {
        try { client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception exception) { throw sanitized(exception); }
    }

    public static ExplorerException sanitized(Exception exception) {
        if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
        if (exception instanceof ErrorResponseException response &&
                ("NoSuchKey".equals(response.errorResponse().code()) || "NoSuchObject".equals(response.errorResponse().code()))) {
            return new ExplorerException(ExplorerException.Code.OBJECT_NOT_FOUND);
        }
        // Do not retain SDK responses, request URLs or credentials in public errors/logs.
        return new ExplorerException(ExplorerException.Code.STORAGE_UNAVAILABLE);
    }
}
