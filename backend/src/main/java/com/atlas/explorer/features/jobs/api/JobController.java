package com.atlas.explorer.features.jobs.api;

import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.features.jobs.service.JobService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/storage/jobs")
@ConditionalOnProperty(name="jobs.enabled",havingValue="true")
public class JobController {
    private final JobService jobs;
    public JobController(JobService jobs) { this.jobs=jobs; }
    @Operation(summary="Lancer une extraction SQL Server vers Parquet ou CSV", requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(
            content=@Content(mediaType="application/json", examples={@ExampleObject(name="Exemple du sujet",value="""
                {"source_id":"sqlserver","table":"Invoice","format":"parquet","partition_by":["invoice_date"]}
                """),@ExampleObject(name="CSV gzip par mois",value="""
                {"source_id":"sqlserver","schema":"dbo","table":"Invoice","format":"csv","compression":"gzip","partition_by":["invoice_date"],"partition_granularity":"month","fetch_size":1000,"num_partitions":4}
                """)})))
    @ApiResponse(responseCode="202",description="Demande acceptee ; consulter le job avec son identifiant",content=@Content(schema=@Schema(implementation=StorageJob.class)))
    @ApiResponse(responseCode="404",description="Source ou table introuvable",content=@Content)
    @ApiResponse(responseCode="422",description="Parametres invalides ; consulter les champs indiques",content=@Content)
    @ApiResponse(responseCode="503",description="Historique des traitements indisponible",content=@Content)
    @PostMapping public ResponseEntity<StorageJob> create(@Valid @RequestBody CreateJobRequest request) {
        var job=jobs.create(request);
        return ResponseEntity.accepted().location(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(job.id()).toUri()).body(job);
    }
    @GetMapping public List<StorageJob> list() { return jobs.list(); }
    @Operation(summary="Verifier les colonnes partitionnables sans creer de traitement")
    @GetMapping("/partition-candidates") public List<Map<String,Object>> partitionCandidates(
            @RequestParam("source_id") String sourceId,@RequestParam(defaultValue="dbo") String schema,
            @RequestParam String table,@RequestParam(name="partition_granularity",defaultValue="month") String granularity) {
        return jobs.partitionCandidates(sourceId,schema,table,granularity);
    }
    @GetMapping("/{id}") public StorageJob get(@PathVariable UUID id) { return jobs.get(id); }
    @GetMapping("/{id}/result") public Map<String,Object> result(@PathVariable UUID id) { return jobs.result(id); }
}
