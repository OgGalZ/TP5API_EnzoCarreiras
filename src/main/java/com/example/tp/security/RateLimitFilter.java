package com.example.tp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Exercice 4 : version maison, max 5 requetes par minute et par IP
@Component
@ConditionalOnProperty(name = "app.security.rate-limit.impl", havingValue = "maison")
public class RateLimitFilter extends OncePerRequestFilter implements LimiteurDeDebit {

    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();
    private final int maxRequetes;
    private final long fenetreMs;
    private final ErrorResponseWriter errorWriter;

    public RateLimitFilter(@Value("${app.security.rate-limit.max-requests}") int maxRequetes,
                           @Value("${app.security.rate-limit.window-ms}") long fenetreMs,
                           ErrorResponseWriter errorWriter) {
        this.maxRequetes = maxRequetes;
        this.fenetreMs = fenetreMs;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // on prend getRemoteAddr et pas X-Forwarded-For que le client peut falsifier
        String ip = request.getRemoteAddr();
        Deque<Long> deque = requests.computeIfAbsent(ip, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        boolean bloque;
        long attente = 0;

        // synchronized car ArrayDeque n'est pas thread-safe
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() < now - fenetreMs) {
                deque.pollFirst();
            }
            bloque = deque.size() >= maxRequetes;
            if (bloque) {
                attente = (deque.peekFirst() + fenetreMs - now) / 1000 + 1;
            } else {
                deque.addLast(now);
            }
        }

        if (bloque) {
            response.setHeader("Retry-After", String.valueOf(attente));
            errorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de requetes, reessayez dans " + attente + " s");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
