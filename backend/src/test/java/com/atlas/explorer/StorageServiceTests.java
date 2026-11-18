package com.atlas.explorer;

import com.atlas.explorer.features.storage.api.StorageController;
import com.atlas.explorer.features.storage.config.StorageProperties;
import com.atlas.explorer.features.storage.connector.MinioObjectStorage;
import com.atlas.explorer.features.storage.connector.ObjectStorage;
import com.atlas.explorer.features.storage.service.StorageService;
import com.atlas.explorer.shared.api.ApiExceptionHandler;
import com.atlas.explorer.shared.domain.ExplorerException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StorageServiceTests {
    @Test void rejectsAmbiguousKeysBeforeAnyStorageOperation() {
        var adapter = mock(ObjectStorage.class);
        var storage = new StorageService(adapter, "lake");
        for (String key : List.of("", "/file", "file/", "a//b", "../file", "a/./b", "a/../b", "a\\b", "a\nb", "é".repeat(513))) {
            assertEquals(ExplorerException.Code.STORAGE_INVALID, assertThrows(ExplorerException.class, () -> storage.stat(key)).code());
        }
        assertThrows(ExplorerException.class, () -> storage.open(null));
        verifyNoInteractions(adapter);
    }

    @Test void rejectsUnknownLengthAndInvalidInputWithoutReadingIt() {
        var adapter = mock(ObjectStorage.class);
        var storage = new StorageService(adapter, "lake");
        var stream = new ByteArrayInputStream(new byte[0]);
        assertThrows(ExplorerException.class, () -> storage.put("source/table/file", stream, -1, "text/plain"));
        assertThrows(ExplorerException.class, () -> storage.put("source/table/file", null, 0, "text/plain"));
        assertThrows(ExplorerException.class, () -> storage.put("source/table/file", stream, 0, "text/plain\r\nprivate"));
        verifyNoInteractions(adapter);
    }

    @Test void uploadPassesThroughTheStreamAndLeavesOwnershipWithCaller() {
        var adapter = mock(ObjectStorage.class);
        var storage = new StorageService(adapter, "lake");
        var stream = mock(java.io.InputStream.class);
        storage.put("sqlserver/dbo/Invoice/job-1/part-0001.csv", stream, 42, "text/csv");
        verify(adapter).put("sqlserver/dbo/Invoice/job-1/part-0001.csv", stream, 42, "text/csv");
        verifyNoInteractions(stream);
    }

    @Test void statusReportsMissingBucketAndConnectionFailureWithoutSecrets() throws Exception {
        var adapter = mock(ObjectStorage.class);
        var storage = new StorageService(adapter, "lake");
        var mvc = MockMvcBuilders.standaloneSetup(new StorageController(storage)).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(get("/storage/status")).andExpect(status().isServiceUnavailable());
        when(adapter.bucketExists()).thenThrow(MinioObjectStorage.sanitized(new IOException("secret-key at private-endpoint")));
        var response = mvc.perform(get("/storage/status")).andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString();
        assertFalse(response.contains("secret-key"));
        assertFalse(response.contains("private-endpoint"));
        doReturn(true).when(adapter).bucketExists();
        mvc.perform(get("/storage/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready")).andExpect(jsonPath("$.bucket").value("lake"));
    }

    @Test void storageCredentialsAndSdkFailuresAreRedacted() {
        assertFalse(new StorageProperties("http://private", "us-east-1", "lake", "access", "secret").toString().contains("secret"));
        var exception = MinioObjectStorage.sanitized(new IOException("private-secret"));
        assertEquals(ExplorerException.Code.STORAGE_UNAVAILABLE, exception.code());
        assertNull(exception.getCause());
        assertFalse(exception.getMessage().contains("private-secret"));
    }
}
