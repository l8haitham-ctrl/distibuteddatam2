package com.atlas.explorer.features.storage.api;

import com.atlas.explorer.features.storage.dto.StorageStatus;
import com.atlas.explorer.features.storage.service.StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "storage.enabled", havingValue = "true")
public class StorageController {
    private final StorageService storage;
    public StorageController(StorageService storage) { this.storage = storage; }
    @GetMapping("/storage/status") public StorageStatus status() { return storage.status(); }
}
