package com.atlas.explorer.features.jobs.api;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice(assignableTypes=JobController.class)
@Order(-1)
public class JobExceptionHandler {
    @ExceptionHandler(com.atlas.explorer.features.jobs.service.InvalidJobException.class)
    public ResponseEntity<Map<String,Object>> invalidSelection(com.atlas.explorer.features.jobs.service.InvalidJobException exception) {
        return ResponseEntity.unprocessableContent().body(Map.of("detail","Parametres du job invalides.","errors",Map.of(exception.field(),exception.getMessage())));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> invalidJob(MethodArgumentNotValidException exception) {
        var errors=new LinkedHashMap<String,String>();
        var messages=Map.of("sourceId","source_id est obligatoire.","table","Utiliser un nom de table valide, par exemple Invoice.",
                "schema","Utiliser un schema valide, par exemple dbo.","format","Utiliser parquet ou csv.",
                "partitionBy","Utiliser au maximum 8 noms de colonnes valides.",
                "fetchSize","fetch_size doit etre compris entre 1 et 100000.","compression","Utiliser snappy, zstd, gzip ou uncompressed.",
                "partitionGranularity","Utiliser month, year ou day.","numPartitions","num_partitions doit etre compris entre 1 et 16.");
        for(var error:exception.getBindingResult().getFieldErrors()) {
            String field=error.getField().replaceAll("\\[.*", "");
            String name=switch(field) {case "sourceId" -> "source_id";case "partitionBy" -> "partition_by";case "fetchSize" -> "fetch_size";case "partitionGranularity" -> "partition_granularity";case "numPartitions" -> "num_partitions";default -> field;};
            errors.put(name,messages.getOrDefault(field,"Parametre invalide."));
        }
        return ResponseEntity.unprocessableContent().body(Map.of("detail","Paramètres du job invalides.","errors",errors));
    }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String,String>> databaseUnavailable() {
        return ResponseEntity.status(503).body(Map.of("detail","Historique des traitements indisponible."));
    }
}
