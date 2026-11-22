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
            case CONNECTOR_NOT_FOUND -> { status = 422; message = "Connecteur non installé ou indisponible."; }
            case AUTHENTICATION -> { status = 400; message = "Authentification refusée : vérifier les identifiants et la base."; }
            case UNAVAILABLE -> { status = 503; message = "Connexion impossible : vérifier le serveur, le port et le certificat."; }
            case SOURCE_NOT_FOUND -> { status = 404; message = "Source inconnue."; }
            case TABLE_NOT_FOUND -> { status = 404; message = "Table introuvable ou non accessible."; }
            case OBJECT_NOT_FOUND -> { status = 404; message = "Fichier introuvable."; }
            case STORAGE_UNAVAILABLE -> { status = 503; message = "Stockage indisponible : vérifier le service et sa configuration."; }
            case STORAGE_INVALID -> { status = 422; message = "Paramètres de stockage invalides."; }
            case JOB_NOT_FOUND -> { status = 404; message = "Traitement introuvable."; }
            case JOB_NOT_COMPLETE -> { status = 409; message = "Le traitement ne dispose pas d’un résultat terminé."; }
            case JOB_ENGINE_UNAVAILABLE -> { status = 503; message = "Moteur de traitement indisponible."; }
            case JOB_RESULT_INVALID -> { status = 502; message = "Résultat du traitement invalide."; }
            default -> { status = 502; message = "La base a refusé l’opération. Vérifier les droits de lecture."; }
        }
        return ResponseEntity.status(status).body(Map.of("detail", message));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalid() {
        // No binding errors or rejected values: these can contain passwords.
        return ResponseEntity.unprocessableContent().body(Map.of("detail", "Paramètres de la demande invalides."));
    }
}
