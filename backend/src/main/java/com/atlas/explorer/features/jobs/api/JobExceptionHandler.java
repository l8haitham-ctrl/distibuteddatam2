package com.atlas.explorer.features.jobs.api;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice(assignableTypes=JobController.class)
public class JobExceptionHandler {
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String,String>> databaseUnavailable() {
        return ResponseEntity.status(503).body(Map.of("detail","Historique des traitements indisponible."));
    }
}
