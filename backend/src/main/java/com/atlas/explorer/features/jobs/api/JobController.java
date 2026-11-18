package com.atlas.explorer.features.jobs.api;

import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.features.jobs.service.JobService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.*;

@RestController
@RequestMapping("/storage/jobs")
@ConditionalOnProperty(name="jobs.enabled",havingValue="true")
public class JobController {
    private final JobService jobs;
    public JobController(JobService jobs) { this.jobs=jobs; }
    @PostMapping public ResponseEntity<StorageJob> create(@Valid @RequestBody CreateJobRequest request) {
        var job=jobs.create(request);
        return ResponseEntity.accepted().location(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(job.id()).toUri()).body(job);
    }
    @GetMapping public List<StorageJob> list() { return jobs.list(); }
    @GetMapping("/{id}") public StorageJob get(@PathVariable UUID id) { return jobs.get(id); }
    @GetMapping("/{id}/result") public Map<String,Object> result(@PathVariable UUID id) { return jobs.result(id); }
}
