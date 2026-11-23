package com.atlas.explorer.features.quality.api;
import com.atlas.explorer.features.quality.dto.QualityRequest;
import com.atlas.explorer.features.quality.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.util.Map;
@RestController
@ConditionalOnProperty(name={"jobs.enabled","catalog.enabled"},havingValue="true")
public class QualityController {
    private final QualityService quality;
    public QualityController(QualityService quality){this.quality=quality;}
    @PostMapping("/quality/reports") public Map<String,Object> report(@Valid @RequestBody QualityRequest request){return quality.report(request);}
    @ExceptionHandler(QualityException.class) public ResponseEntity<Map<String,String>> failed(QualityException error){return ResponseEntity.status(error.invalid()?422:503).body(Map.of("detail",error.getMessage()));}
}
