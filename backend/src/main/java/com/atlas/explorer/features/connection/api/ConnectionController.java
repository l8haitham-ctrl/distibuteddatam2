package com.atlas.explorer.features.connection.api;

import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connection.service.ConnectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/connections")
public class ConnectionController {
    private final ConnectionService service;
    public ConnectionController(ConnectionService service) { this.service = service; }
    @PostMapping("/test") public Map<String, Object> test(@Valid @RequestBody ConnectionConfig config) { return service.test(config); }
}
