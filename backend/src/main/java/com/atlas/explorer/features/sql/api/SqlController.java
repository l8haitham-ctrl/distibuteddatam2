package com.atlas.explorer.features.sql.api;
import com.atlas.explorer.features.sql.dto.SqlRequest;
import com.atlas.explorer.features.sql.service.SqlService;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import java.util.Map;
@RestController
@ConditionalOnProperty(name="catalog.enabled",havingValue="true")
public class SqlController {
    private final SqlService service;
    public SqlController(SqlService service){this.service=service;}
    @Operation(summary="Executer une requete Trino en lecture seule",requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(
        content=@Content(mediaType="application/json",examples=@ExampleObject(value="{\"sql\":\"SELECT * FROM hive.sqlserver_dbo.invoice LIMIT 100\",\"max_rows\":1000}"))))
    @PostMapping("/sql/query") public Map<String,Object> query(@Valid @RequestBody SqlRequest request){return service.query(request);}
}
