package com.atlas.explorer.features.catalog.api;
import com.atlas.explorer.features.catalog.service.CatalogException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.core.annotation.Order;
import java.util.Map;
@RestControllerAdvice(assignableTypes=CatalogController.class)
@Order(-1)
public class CatalogExceptionHandler {
    @ExceptionHandler({CatalogException.class,DataAccessException.class}) public ResponseEntity<?> unavailable(){return ResponseEntity.status(503).body(Map.of("detail","Catalogue indisponible."));}
}
