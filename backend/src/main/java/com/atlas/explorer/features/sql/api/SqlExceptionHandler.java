package com.atlas.explorer.features.sql.api;
import com.atlas.explorer.features.sql.service.SqlException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.annotation.Order;
import java.util.Map;
@RestControllerAdvice(assignableTypes=SqlController.class)
@Order(-1)
public class SqlExceptionHandler {
    @ExceptionHandler(SqlException.class) public ResponseEntity<?> sql(SqlException e){return ResponseEntity.status(e.unavailable()?503:422).body(Map.of("detail",e.getMessage()));}
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<?> invalid(){return ResponseEntity.unprocessableContent().body(Map.of("detail","sql est obligatoire (20000 caracteres maximum), max_rows doit etre compris entre 1 et 1000."));}
}
