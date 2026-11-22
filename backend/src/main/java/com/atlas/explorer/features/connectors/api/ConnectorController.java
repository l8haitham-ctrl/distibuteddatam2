package com.atlas.explorer.features.connectors.api;
import com.atlas.explorer.features.connectors.service.ConnectorService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class ConnectorController {
    private final ConnectorService service;
    public ConnectorController(ConnectorService service){this.service=service;}
    @GetMapping("/connectors") public List<Map<String,Object>> list(){return service.list();}
}
