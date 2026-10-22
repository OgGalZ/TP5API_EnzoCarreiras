package com.example.tp.model;

import java.time.LocalDateTime;

// format commun pour toutes les erreurs renvoyees par l'API
public record ApiError(String timestamp, int status, String error, String message, String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now().toString(), status, error, message, path);
    }
}
