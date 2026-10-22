package com.example.tp.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Exercice 2 : lit le JWT et met l'utilisateur + son role dans le contexte Spring Security.
// Les droits d'acces sont definis dans SecurityConfig.
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ErrorResponseWriter errorWriter;

    public JwtFilter(JwtUtil jwtUtil, ErrorResponseWriter errorWriter) {
        this.jwtUtil = jwtUtil;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // pas de token : on laisse passer, Spring renverra 401 si la route est protegee
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtUtil.lireClaims(header.substring(7));
        if (claims == null || claims.getSubject() == null || claims.get("role") == null) {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Token JWT invalide ou expire");
            return;
        }

        // "admin" -> ROLE_ADMIN, ce qui correspond a hasRole("ADMIN")
        String role = "ROLE_" + claims.get("role", String.class).toUpperCase();
        var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null,
                List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
