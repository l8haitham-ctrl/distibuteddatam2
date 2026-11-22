package com.atlas.explorer.features.exploration.api;

import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.exploration.service.ExplorationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class ExplorationController {
    private final SourceService sources;
    private final ExplorationService exploration;
    public ExplorationController(SourceService sources, ExplorationService exploration) { this.sources = sources; this.exploration = exploration; }
    @GetMapping("/sources/{id}/tables") public Map<String, Object> tables(@PathVariable String id) { return exploration.tables(sources.get(id)); }
    @GetMapping("/sources/{id}/tables/{table}") public Map<String, Object> describe(@PathVariable String id, @PathVariable String table, @RequestParam(required=false) String schema) {
        return exploration.describe(sources.get(id), schema==null?sources.defaultSchema(id):schema, table);
    }
    @PostMapping("/connections/explore") public Map<String, Object> explore(@Valid @RequestBody ConnectionConfig config) { return exploration.explore(config); }
}
