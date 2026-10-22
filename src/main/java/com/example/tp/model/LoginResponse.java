package com.example.tp.model;

public record LoginResponse(String token, String type, long expiresInSeconds, String role) {
}
