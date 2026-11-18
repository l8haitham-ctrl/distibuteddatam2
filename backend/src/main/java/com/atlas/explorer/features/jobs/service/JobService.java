package com.atlas.explorer.features.jobs.service;

import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import com.atlas.explorer.features.jobs.connector.JobRepository;
import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.shared.domain.ExplorerException;
import java.util.*;

public class JobService {
    private final JobRepository repository;
    private final SourceService sources;
    private final ExplorationService exploration;
    public JobService(JobRepository repository,SourceService sources,ExplorationService exploration) {
        this.repository=repository;this.sources=sources;this.exploration=exploration;
    }
    public StorageJob create(CreateJobRequest request) {
        // Confirm source/table access before persisting any executable request.
        exploration.describe(sources.get(request.sourceId()),request.schema(),request.table());
        return repository.create(request);
    }
    public StorageJob get(UUID id) { return repository.get(id); }
    public List<StorageJob> list() { return repository.list(); }
    public Map<String,Object> result(UUID id) {
        var job=get(id);
        if(!job.status().equals("COMPLETED"))throw new ExplorerException(ExplorerException.Code.JOB_NOT_COMPLETE);
        return job.result();
    }
}
