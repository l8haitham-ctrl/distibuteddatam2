package com.atlas.explorer.features.nativesources.api;

import com.atlas.explorer.features.nativesources.service.NativeSourceException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class NativeSourceExceptionHandler {
    @ExceptionHandler(NativeSourceException.class)
    public ResponseEntity<Map<String,String>> failed(NativeSourceException error){return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("detail",error.getMessage()));}
}
