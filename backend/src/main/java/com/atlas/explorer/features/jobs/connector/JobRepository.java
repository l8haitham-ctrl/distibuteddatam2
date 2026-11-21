package com.atlas.explorer.features.jobs.connector;

import com.atlas.explorer.features.jobs.dto.*;
import java.util.*;

public interface JobRepository {
    StorageJob create(CreateJobRequest request);
    StorageJob get(UUID id);
    List<StorageJob> list();
    Optional<StorageJob> claim();
    List<StorageJob> active();
    void running(UUID id, String submissionId);
    void registering(UUID id, Map<String,Object> manifest);
    Map<String,Object> pending(UUID id);
    void registrationError(UUID id);
    void completed(UUID id, Map<String,Object> result);
    void failed(UUID id, String code);
}
