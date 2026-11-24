package com.atlas.explorer;

import com.atlas.explorer.features.storage.config.StorageProperties;
import com.atlas.explorer.features.storage.service.StorageService;
import com.atlas.explorer.shared.domain.ExplorerException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "RUN_STORAGE_TESTS", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StorageIntegrationTests {
    @Autowired StorageService storage;
    @Autowired StorageProperties properties;
    @Autowired MinioClient client;
    @LocalServerPort int port;

    private String key(String file) { return "_integration/" + UUID.randomUUID() + "/" + file; }

    @Test void fileRoundTripPreservesBytesMetadataAndCleanup() throws Exception {
        String key = key("sample.csv");
        byte[] bytes = "invoice_id,amount\n1,120.50\n2,90.00\n".getBytes(StandardCharsets.UTF_8);
        try {
            try (var input = new ByteArrayInputStream(bytes)) {
                var stored = storage.put(key, input, bytes.length, "text/csv");
                assertEquals(properties.bucket(), stored.bucket());
                assertEquals(key, stored.key());
                assertFalse(stored.etag().isBlank());
            }
            var metadata = storage.stat(key);
            assertEquals(bytes.length, metadata.size());
            assertEquals("text/csv", metadata.contentType());
            try (var downloaded = storage.open(key)) { assertArrayEquals(bytes, downloaded.readAllBytes()); }
        } finally { storage.delete(key); }
        assertEquals(ExplorerException.Code.OBJECT_NOT_FOUND, assertThrows(ExplorerException.class, () -> storage.open(key)).code());
        assertEquals(ExplorerException.Code.OBJECT_NOT_FOUND, assertThrows(ExplorerException.class, () -> storage.stat(key)).code());
    }

    @Test void multipartUploadStreamsMoreThanOneChunkWithoutLoadingFile() throws Exception {
        String key = key("multipart.bin");
        long size = 6L * 1024 * 1024 + 17;
        var originalHash = MessageDigest.getInstance("SHA-256");
        var downloadHash = MessageDigest.getInstance("SHA-256");
        // Generate the fixture as it is read; never allocate a file-sized byte array.
        var generated = new InputStream() {
            long position;
            @Override public int read() { return position >= size ? -1 : (int) ((position++ * 31) & 255); }
            @Override public int read(byte[] buffer, int offset, int length) {
                if (position >= size) return -1;
                int count = (int) Math.min(length, size - position);
                for (int i = 0; i < count; i++) buffer[offset + i] = (byte) (position++ * 31);
                return count;
            }
        };
        try {
            try (var input = new DigestInputStream(generated, originalHash)) { storage.put(key, input, size, "application/octet-stream"); }
            assertEquals(size, storage.stat(key).size());
            try (var input = new DigestInputStream(storage.open(key), downloadHash)) {
                assertEquals(size, input.transferTo(java.io.OutputStream.nullOutputStream()));
            }
            assertArrayEquals(originalHash.digest(), downloadHash.digest());
        } finally { storage.delete(key); }
    }

    @Test void emptyObjectWorks() throws Exception {
        String key = key("empty.csv");
        try {
            storage.put(key, new ByteArrayInputStream(new byte[0]), 0, "text/csv");
            assertEquals(0, storage.stat(key).size());
            try (var input = storage.open(key)) { assertEquals(-1, input.read()); }
        } finally { storage.delete(key); }
    }

    @Test void applicationCannotCreateBucketsAndObjectsAreNotPublic() throws Exception {
        var denied = assertThrows(ErrorResponseException.class, () -> client.makeBucket(MakeBucketArgs.builder()
                .bucket("denied-" + UUID.randomUUID()).build()));
        assertEquals("AccessDenied", denied.errorResponse().code());
        String key = key("private.txt");
        try {
            storage.put(key, new ByteArrayInputStream(new byte[]{1}), 1, "text/plain");
            try (var anonymous = MinioClient.builder().endpoint(properties.endpoint()).region(properties.region()).build()) {
                var error = assertThrows(ErrorResponseException.class, () -> anonymous.getObject(GetObjectArgs.builder()
                        .bucket(properties.bucket()).object(key).build()));
                assertEquals("AccessDenied", error.errorResponse().code());
            }
        } finally { storage.delete(key); }
    }

    @Test void statusEndpointIsAvailableAndContainsNoCredentials() throws Exception {
        var response = com.atlas.explorer.support.AuthenticatedHttp.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/storage/status"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("ready"));
        assertFalse(response.body().contains(properties.accessKey()));
        assertFalse(response.body().contains(properties.secretKey()));
    }
}
