package com.atlas.explorer.features.nativesources.api;

import com.atlas.explorer.features.nativesources.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/native/sources")
public class NativeConfiguredSourceController {
    private final NativeSourceProfiles profiles;
    private final NativeSourceService service;
    public NativeConfiguredSourceController(NativeSourceProfiles profiles,NativeSourceService service){this.profiles=profiles;this.service=service;}
    @GetMapping public List<Map<String,Object>> sources(){return profiles.list();}
    @PostMapping("/{id}/test") public Map<String,Object> test(@PathVariable String id){return service.test(profiles.get(id));}
    @PostMapping("/{id}/explore") public Map<String,Object> explore(@PathVariable String id){return service.explore(profiles.get(id));}
}
