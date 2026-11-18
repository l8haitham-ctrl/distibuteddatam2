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
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> invalidJob(MethodArgumentNotValidException exception) {
        var errors=new LinkedHashMap<String,String>();
        var messages=Map.of("sourceId","source_id est obligatoire.","table","Utiliser un nom de table valide, par exemple Invoice.",
                "schema","Utiliser un schema valide, par exemple dbo.","format","Seul parquet est disponible.",
                "partitionBy","Utiliser partition_by: [] ; le partitionnement sera ajoute ensuite.",
                "fetchSize","fetch_size doit etre compris entre 1 et 100000.","compression","Utiliser snappy ou uncompressed.");
        for(var error:exception.getBindingResult().getFieldErrors()) {
            String field=error.getField();
            String name=switch(field) {case "sourceId" -> "source_id";case "partitionBy" -> "partition_by";case "fetchSize" -> "fetch_size";default -> field;};
            errors.put(name,messages.getOrDefault(field,"Parametre invalide."));
        }
        return ResponseEntity.unprocessableContent().body(Map.of("detail","Paramètres du job invalides.","errors",errors));
    }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String,String>> databaseUnavailable() {
        return ResponseEntity.status(503).body(Map.of("detail","Historique des traitements indisponible."));
    }
}
