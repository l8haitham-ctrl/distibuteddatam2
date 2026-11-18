package com.atlas.explorer.features.storage.connector;

import com.atlas.explorer.features.storage.dto.StoredObject;
import java.io.InputStream;

/** Storage port independent of the S3 SDK, HTTP and future ingestion jobs. */
public interface ObjectStorage {
    boolean bucketExists();
    StoredObject put(String key, InputStream content, long size, String contentType);
    StoredObject stat(String key);
    /** The caller must close the returned stream, including on read failure. */
    InputStream open(String key);
    void delete(String key);
}
