package com.atlas.explorer.shared.api;

import com.atlas.explorer.shared.domain.ExplorerException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ExplorerException.class)
    public ResponseEntity<Map<String, String>> domain(ExplorerException exception) {
        int status; String message;
        switch (exception.code()) {
            case AUTHENTICATION -> { status = 400; message = "Authentification refusée : vérifier les identifiants et la base."; }
            case UNAVAILABLE -> { status = 503; message = "Connexion impossible : vérifier le serveur, le port et le certificat."; }
            case SOURCE_NOT_FOUND -> { status = 404; message = "Source inconnue."; }
            case TABLE_NOT_FOUND -> { status = 404; message = "Table introuvable ou non accessible."; }
            default -> { status = 502; message = "La base a refusé l’opération. Vérifier les droits de lecture."; }
        }
        return ResponseEntity.status(status).body(Map.of("detail", message));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalid() {
        // No binding errors or rejected values: these can contain passwords.
        return ResponseEntity.unprocessableContent().body(Map.of("detail", "Paramètres de connexion invalides."));
    }
}
