package com.example.tp.exception;

import com.example.tp.model.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // on renvoie toutes les erreurs de champ, pas seulement la premiere
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " : " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return erreur(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> parametreInvalide(Exception ex, HttpServletRequest req) {
        return erreur(HttpStatus.BAD_REQUEST, "Parametre de requete invalide ou manquant", req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonInvalide(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return erreur(HttpStatus.BAD_REQUEST, "JSON invalide", req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> mauvaisIdentifiants(BadCredentialsException ex, HttpServletRequest req) {
        return erreur(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler({RessourceIntrouvableException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> introuvable(Exception ex, HttpServletRequest req) {
        return erreur(HttpStatus.NOT_FOUND, "Ressource introuvable", req);
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ApiError> conflit(ConflitException ex, HttpServletRequest req) {
        return erreur(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methodeNonSupportee(Exception ex, HttpServletRequest req) {
        return erreur(HttpStatus.METHOD_NOT_ALLOWED, "Methode non supportee", req);
    }

    // pour tout le reste : on log l'erreur mais on ne renvoie jamais la stack trace au client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> autre(Exception ex, HttpServletRequest req) {
        log.error("Erreur sur " + req.getRequestURI(), ex);
        return erreur(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", req);
    }

    private ResponseEntity<ApiError> erreur(HttpStatus status, String message, HttpServletRequest req) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
