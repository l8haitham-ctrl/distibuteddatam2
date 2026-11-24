package com.atlas.explorer.features.glossary.api;

import com.atlas.explorer.features.glossary.dto.*;
import com.atlas.explorer.features.glossary.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataAccessException;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/glossary")
@ConditionalOnProperty(name="jobs.enabled",havingValue="true")
public class GlossaryController {
    private final GlossaryService service;
    public GlossaryController(GlossaryService service) { this.service=service; }
    @PostMapping("/terms") public ResponseEntity<BusinessTerm> create(@Valid @RequestBody TermRequest request) {
        var term=service.create(request);return ResponseEntity.created(URI.create("/glossary/terms/"+term.id())).body(term);
    }
    @GetMapping("/terms") public List<BusinessTerm> list(@RequestParam(defaultValue="") @Size(max=160) String q,
            @RequestParam(defaultValue="50") @Min(1) @Max(100) int limit,@RequestParam(defaultValue="0") @Min(0) int offset) {
        return service.list(q,limit,offset);
    }
    @GetMapping("/terms/{id}") public BusinessTerm get(@PathVariable UUID id) { return service.get(id); }
    @PutMapping("/terms/{id}") public BusinessTerm update(@PathVariable UUID id,@Valid @RequestBody TermRequest request) { return service.update(id,request); }
    @PostMapping("/terms/{id}/links") public ResponseEntity<TermLink> link(@PathVariable UUID id,@Valid @RequestBody LinkRequest target) {
        return ResponseEntity.status(201).body(service.link(id,target));
    }
    @DeleteMapping("/terms/{id}/links/{linkId}") public ResponseEntity<Void> unlink(@PathVariable UUID id,@PathVariable UUID linkId) {
        service.unlink(id,linkId);return ResponseEntity.noContent().build();
    }
    @GetMapping("/links") public List<TermLink> lookup(
            @RequestParam("source_id") @NotBlank @Size(max=128) String source,
            @RequestParam("schema_name") @NotBlank @Size(max=128) String schema,
            @RequestParam("table_name") @NotBlank @Size(max=128) String table,
            @RequestParam(value="column_name",required=false) @Pattern(regexp="(?s).*\\S.*") @Size(max=128) String column,
            @RequestParam(defaultValue="50") @Min(1) @Max(100) int limit,@RequestParam(defaultValue="0") @Min(0) int offset) {
        return service.lookup(new LinkRequest(source,schema,table,column),limit,offset);
    }
    @ExceptionHandler(GlossaryException.class) public ResponseEntity<Map<String,String>> domain(GlossaryException e) {
        int status=switch(e.code()) { case NOT_FOUND->404;case CONFLICT->409;case INVALID_TARGET->422; };
        return ResponseEntity.status(status).body(Map.of("detail",e.getMessage()));
    }
    @ExceptionHandler(DataAccessException.class) public ResponseEntity<Map<String,String>> unavailable() {
        return ResponseEntity.status(503).body(Map.of("detail","Metadonnees du glossaire indisponibles."));
    }
    @ExceptionHandler({HandlerMethodValidationException.class,MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String,String>> invalid() { return ResponseEntity.unprocessableContent().body(Map.of("detail","Parametres du glossaire invalides.")); }
}
