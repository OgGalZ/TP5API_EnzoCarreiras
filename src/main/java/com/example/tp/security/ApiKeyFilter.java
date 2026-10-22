package com.example.tp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

// Exercice 1 : verifie la cle API sur les routes /api/**
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final byte[] cleValide;
    private final ErrorResponseWriter errorWriter;

    public ApiKeyFilter(@Value("${app.security.api-key}") String apiKey, ErrorResponseWriter errorWriter) {
        this.cleValide = ("ApiKey " + apiKey).getBytes(StandardCharsets.UTF_8);
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getHeader("Authorization");

        // MessageDigest.isEqual compare en temps constant (contrairement a equals)
        if (key == null || !MessageDigest.isEqual(cleValide, key.getBytes(StandardCharsets.UTF_8))) {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Cle API absente ou invalide");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken("api-client", null,
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
