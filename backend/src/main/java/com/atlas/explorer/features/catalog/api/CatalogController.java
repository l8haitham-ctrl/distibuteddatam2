package com.atlas.explorer.features.catalog.api;
import com.atlas.explorer.features.catalog.service.CatalogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.*;
@RestController
@ConditionalOnProperty(name="catalog.enabled",havingValue="true")
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service){this.service=service;}
    @GetMapping("/catalog/tables") public List<Map<String,Object>> tables(){return service.tables();}
}
