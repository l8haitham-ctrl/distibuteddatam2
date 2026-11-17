package com.atlas.explorer.features.connection.api;

import com.atlas.explorer.features.connection.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/sources")
public class SourceController {
    private final SourceService sources;
    private final ConnectionService connections;
    public SourceController(SourceService sources, ConnectionService connections) { this.sources = sources; this.connections = connections; }
    @GetMapping public List<Map<String, Object>> list() { return sources.list(); }
    @PostMapping("/{id}/test") public Map<String, Object> test(@PathVariable String id) { return connections.test(sources.get(id)); }
}
