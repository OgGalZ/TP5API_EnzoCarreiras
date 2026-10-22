package com.example.tp.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
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
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Bonus : meme limite mais avec Bucket4j (token bucket).
// Chaque IP a un seau de 5 jetons qui se recharge de 5 jetons par minute.
@Component
@ConditionalOnProperty(name = "app.security.rate-limit.impl", havingValue = "bucket4j", matchIfMissing = true)
public class Bucket4jRateLimitFilter extends OncePerRequestFilter implements LimiteurDeDebit {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxRequetes;
    private final Duration fenetre;
    private final ErrorResponseWriter errorWriter;

    public Bucket4jRateLimitFilter(@Value("${app.security.rate-limit.max-requests}") int maxRequetes,
                                   @Value("${app.security.rate-limit.window-ms}") long fenetreMs,
                                   ErrorResponseWriter errorWriter) {
        this.maxRequetes = maxRequetes;
        this.fenetre = Duration.ofMillis(fenetreMs);
        this.errorWriter = errorWriter;
    }

    private Bucket creerBucket() {
        Bandwidth limite = Bandwidth.builder()
                .capacity(maxRequetes)
                .refillGreedy(maxRequetes, fenetre)
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Bucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(), ip -> creerBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long attente = probe.getNanosToWaitForRefill() / 1_000_000_000 + 1;
            response.setHeader("Retry-After", String.valueOf(attente));
            errorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de requetes, reessayez dans " + attente + " s");
            return;
        }

        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
        filterChain.doFilter(request, response);
    }
}
