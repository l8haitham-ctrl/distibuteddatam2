package com.atlas.explorer.features.jobs.service;

import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import com.atlas.explorer.features.jobs.connector.JobRepository;
import com.atlas.explorer.features.jobs.connector.PartitionCardinality;
import com.atlas.explorer.features.jobs.dto.*;
import com.atlas.explorer.shared.domain.ExplorerException;
import java.util.*;

public class JobService {
    private final JobRepository repository;
    private final SourceService sources;
    private final ExplorationService exploration;
    private final PartitionCardinality cardinality;
    public JobService(JobRepository repository,SourceService sources,ExplorationService exploration,PartitionCardinality cardinality) {
        this.repository=repository;this.sources=sources;this.exploration=exploration;this.cardinality=cardinality;
    }
    public StorageJob create(CreateJobRequest request) {
        if(request.schema()==null)request=new CreateJobRequest(request.sourceId(),sources.defaultSchema(request.sourceId()),request.table(),request.format(),request.partitionBy(),request.fetchSize(),request.compression(),request.partitionGranularity(),request.numPartitions());
        validate(request);
        return repository.create(request);
    }
    public List<Map<String,Object>> partitionCandidates(String sourceId,String schema,String table,String granularity) {
        if(schema==null)schema=sources.defaultSchema(sourceId);
        if(!schema.matches("[A-Za-z_][A-Za-z0-9_]{0,127}") || !table.matches("[A-Za-z_][A-Za-z0-9_]{0,127}")
                || !Set.of("month","year","day").contains(granularity))throw new InvalidJobException("partition_by","Schema, table ou granularite invalide.");
        var metadata=exploration.describe(sources.get(sourceId),schema,table);
        @SuppressWarnings("unchecked") var columns=(List<Map<String,Object>>)metadata.get("columns");
        var result=new ArrayList<Map<String,Object>>();
        for(var column:columns) {
            String name=column.get("name").toString();boolean eligible=false;String reason="Nom de colonne incompatible avec l'extraction.";
            if(name.matches("[A-Za-z_][A-Za-z0-9_]{0,127}"))try {
                validate(new CreateJobRequest(sourceId,schema,table,"parquet",List.of(name),1000,"snappy",granularity,4));
                eligible=true;reason="";
            } catch(InvalidJobException invalid) {reason=invalid.getMessage();}
            result.add(Map.of("name",name,"data_type",column.get("data_type"),"eligible",eligible,"reason",reason));
        }
        return result;
    }
    private void validate(CreateJobRequest request) {
        // Confirm source/table access before persisting any executable request.
        var allowed=request.format().equals("csv")?Set.of("gzip","uncompressed"):Set.of("snappy","zstd","uncompressed");
        if(!allowed.contains(request.compression()))throw new InvalidJobException("compression",
                "Combinaison invalide : parquet accepte snappy, zstd, uncompressed ; csv accepte gzip, uncompressed.");
        var source=sources.get(request.sourceId());
        var metadata=exploration.describe(source,request.schema(),request.table());
        @SuppressWarnings("unchecked") var columns=(List<Map<String,Object>>)metadata.get("columns");
        @SuppressWarnings("unchecked") var keys=(List<Map<String,Object>>)metadata.get("primary_key");
        var names=new HashSet<String>();
        var outputNames=new HashSet<String>();
        columns.forEach(c->outputNames.add(c.get("name").toString().toLowerCase(Locale.ROOT)));
        for(String partition:request.partitionBy()) {
            if(!names.add(partition.toLowerCase(Locale.ROOT)))throw new InvalidJobException("partition_by","Une colonne ne peut etre selectionnee qu'une fois.");
            var column=columns.stream().filter(c->partition.equals(c.get("name"))).findFirst()
                    .orElseThrow(()->new InvalidJobException("partition_by","Colonne de partition inexistante ; utiliser le nom exact affiche dans la structure."));
            if(keys.stream().anyMatch(k->partition.equals(k.get("column_name"))))throw new InvalidJobException("partition_by","Le partitionnement sur une cle primaire est interdit.");
            String type=column.get("data_type").toString();
            if(Boolean.FALSE.equals(column.get("partitionable")) || (!column.containsKey("partitionable") && Set.of("text","ntext","image","xml","varbinary","binary","geography","geometry").contains(type)))
                throw new InvalidJobException("partition_by","Type de colonne incompatible avec le partitionnement.");
            if(cardinality.distinct(source,request.schema(),request.table(),partition)>1000)
                throw new InvalidJobException("partition_by","La colonne depasse la limite de 1000 valeurs distinctes.");
            if(Boolean.TRUE.equals(column.get("temporal")) || Set.of("date","datetime","datetime2","smalldatetime","datetimeoffset").contains(type)) {
                var suffixes=switch(request.partitionGranularity()) {case "year" -> List.of("year");case "day" -> List.of("year","month","day");default -> List.of("year","month");};
                for(String suffix:suffixes)if(!outputNames.add((partition+"_"+suffix).toLowerCase(Locale.ROOT)))
                    throw new InvalidJobException("partition_by","Une colonne derivee entre en conflit avec une colonne existante.");
            }
        }
    }
    public StorageJob get(UUID id) { return repository.get(id); }
    public List<StorageJob> list() { return repository.list(); }
    public Map<String,Object> result(UUID id) {
        var job=get(id);
        if(!job.status().equals("COMPLETED"))throw new ExplorerException(ExplorerException.Code.JOB_NOT_COMPLETE);
        return job.result();
    }
}
