package com.example.tp.security;

import com.example.tp.model.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Les filtres passent avant les controleurs donc le @RestControllerAdvice ne voit pas leurs erreurs.
// Cette classe sert a renvoyer le meme JSON que GlobalExceptionHandler depuis un filtre.
@Component
public class ErrorResponseWriter {

    private final ObjectMapper mapper;

    public ErrorResponseWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ApiError erreur = ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        mapper.writeValue(response.getOutputStream(), erreur);
    }
}
