package com.atlas.explorer.features.jobs.connector;

import com.atlas.explorer.features.jobs.config.JobProperties;
import com.atlas.explorer.features.jobs.dto.StorageJob;
import com.atlas.explorer.shared.domain.ExplorerException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public class SparkJobEngine implements JobEngine {
    private final JobProperties properties;
    private final ObjectMapper json=new ObjectMapper();
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public SparkJobEngine(JobProperties properties) { this.properties=properties; }
    public String submit(StorageJob job) {
        var request=job.request();
        var response=call("POST","/create",Map.of(
                "action","CreateSubmissionRequest","clientSparkVersion","4.3.0",
                "appResource","file:/opt/atlas/atlas-ingestion.jar","mainClass","com.atlas.ingestion.IngestionJob",
                "appArgs",List.of(job.id().toString(),request.sourceId(),request.schema(),request.table(),request.fetchSize().toString(),request.compression(),
                        request.format(),String.join(",",request.partitionBy()),request.partitionGranularity(),request.numPartitions().toString()),
                "environmentVariables",Map.of(),
                "sparkProperties",Map.of("spark.master",properties.sparkMaster(),"spark.app.name","atlas-"+job.id(),
                        "spark.submit.deployMode","cluster","spark.driver.memory","512m","spark.driver.cores","1",
                        "spark.executor.memory","512m","spark.executor.cores","1","spark.cores.max","1",
                        "spark.jars","local:/opt/atlas/atlas-ingestion.jar","spark.redaction.regex","(?i)secret|password|token|access.?key|credentials")));
        if(!Boolean.TRUE.equals(response.get("success")) || !(response.get("submissionId") instanceof String id) || !id.matches("driver-[A-Za-z0-9-]+")) throw unavailable();
        return id;
    }
    public String state(String id) {
        var response=call("GET","/status/"+safeId(id),null);
        return Boolean.TRUE.equals(response.get("success"))?Objects.toString(response.get("driverState"),"UNKNOWN"):"UNKNOWN";
    }
    public void kill(String id) { call("POST","/kill/"+safeId(id),Map.of()); }
    private String safeId(String id) { if(id==null||!id.matches("driver-[A-Za-z0-9-]+"))throw unavailable();return id; }
    private Map<String,Object> call(String method,String path,Object body) {
        try {
            var builder=HttpRequest.newBuilder(URI.create(properties.sparkRestUrl()+"/v1/submissions"+path)).timeout(Duration.ofSeconds(15));
            if(method.equals("GET"))builder.GET();
            else builder.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200 || response.body().length()>65536)throw unavailable();
            return json.readValue(response.body(),new TypeReference<Map<String,Object>>(){});
        } catch(InterruptedException exception) { Thread.currentThread().interrupt();throw unavailable(); }
        catch(Exception exception) { throw unavailable(); }
    }
    private ExplorerException unavailable() { return new ExplorerException(ExplorerException.Code.JOB_ENGINE_UNAVAILABLE); }
}
