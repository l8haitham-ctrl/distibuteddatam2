package com.atlas.explorer.features.jobs.connector;

import com.atlas.explorer.features.jobs.dto.StorageJob;

public interface JobEngine {
    String submit(StorageJob job);
    String state(String submissionId);
    void kill(String submissionId);
}
