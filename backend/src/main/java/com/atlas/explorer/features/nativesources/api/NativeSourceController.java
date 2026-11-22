package com.atlas.explorer.features.nativesources.api;

import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import com.atlas.explorer.features.nativesources.service.NativeSourceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/native")
public class NativeSourceController {
    private final NativeSourceService service;
    public NativeSourceController(NativeSourceService service){this.service=service;}
    @GetMapping("/connectors") public List<Map<String,Object>> connectors(){return service.list();}
    @PostMapping("/connections/test") public Map<String,Object> test(@Valid @RequestBody NativeConnection config){return service.test(config);}
    @PostMapping("/connections/explore") public Map<String,Object> explore(@Valid @RequestBody NativeConnection config){return service.explore(config);}
}
