package com.example.tp.security;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Chaine pour Swagger : publique, et avec une CSP moins stricte sinon la page ne s'affiche pas
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self' 'unsafe-inline'; "
                                + "style-src 'self' 'unsafe-inline'; img-src 'self' data:")))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // Chaine de l'API : rate limit -> cle API -> JWT -> regles d'acces
    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http,
                                        LimiteurDeDebit rateLimitFilter,
                                        ApiKeyFilter apiKeyFilter,
                                        JwtFilter jwtFilter,
                                        RestAuthenticationEntryPoint entryPoint,
                                        RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // pas de session ni de cookie (auth par header) donc pas besoin de protection CSRF
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers("/api/**").hasRole("API_CLIENT")
                        .requestMatchers("/utilisateurs/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/utilisateurs").hasRole("ADMIN")
                        .requestMatchers("/utilisateurs", "/utilisateurs/**").authenticated()
                        .anyRequest().denyAll()) // tout le reste est refuse par defaut
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // ajoutes dans cet ordre au meme endroit, donc executes dans cet ordre
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Comme les filtres sont des @Component, Spring Boot les ajoute aussi automatiquement
    // en tant que filtres servlet. On desactive ca pour qu'ils tournent uniquement dans la chaine.
    @Bean
    public FilterRegistrationBean<LimiteurDeDebit> desactiverRateLimit(LimiteurDeDebit f) {
        return desactiver(f);
    }

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> desactiverApiKey(ApiKeyFilter f) {
        return desactiver(f);
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> desactiverJwt(JwtFilter f) {
        return desactiver(f);
    }

    private <T extends Filter> FilterRegistrationBean<T> desactiver(T filter) {
        FilterRegistrationBean<T> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
