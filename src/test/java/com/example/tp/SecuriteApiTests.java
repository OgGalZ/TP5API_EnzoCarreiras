package com.example.tp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// tests du tableau de l'exercice 6 + quelques cas en plus
// chaque test utilise une IP differente sinon le rate limit bloque les tests suivants
@SpringBootTest
@AutoConfigureMockMvc
class SecuriteApiTests {

    private static final AtomicInteger COMPTEUR_IP = new AtomicInteger(1);

    @Autowired
    private MockMvc mvc;

    private static RequestPostProcessor ip(String adresse) {
        return req -> {
            req.setRemoteAddr(adresse);
            return req;
        };
    }

    private static String nouvelleIp() {
        return "10.0.0." + COMPTEUR_IP.getAndIncrement();
    }

    private String login(String username, String password, String adresse) throws Exception {
        String body = mvc.perform(post("/auth/login").with(ip(adresse))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    // Exercice 1 : cle API

    @Test
    @DisplayName("Sans cle API -> 401")
    void sansCleApi() throws Exception {
        mvc.perform(get("/api/utilisateurs").with(ip(nouvelleIp())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Cle API absente ou invalide"));
    }

    @Test
    @DisplayName("Cle API incorrecte -> 401")
    void cleApiIncorrecte() throws Exception {
        mvc.perform(get("/api/utilisateurs").with(ip(nouvelleIp()))
                        .header("Authorization", "ApiKey mauvaise-cle"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Avec cle API valide -> 200")
    void avecCleApiValide() throws Exception {
        mvc.perform(get("/api/utilisateurs").with(ip(nouvelleIp()))
                        .header("Authorization", "ApiKey 123456abcdef"))
                .andExpect(status().isOk());
    }

    // Exercice 2 : JWT et roles

    @Test
    @DisplayName("JWT user sur /utilisateurs/user -> 200")
    void jwtUserSurUser() throws Exception {
        String adresse = nouvelleIp();
        String token = login("user", "user123", adresse);
        mvc.perform(get("/utilisateurs/user").with(ip(adresse)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilisateur").value("user"));
    }

    @Test
    @DisplayName("JWT user sur /utilisateurs/admin -> 403")
    void jwtUserSurAdmin() throws Exception {
        String adresse = nouvelleIp();
        String token = login("user", "user123", adresse);
        mvc.perform(get("/utilisateurs/admin").with(ip(adresse)).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("JWT admin sur /utilisateurs/admin -> 200")
    void jwtAdminSurAdmin() throws Exception {
        String adresse = nouvelleIp();
        String token = login("admin", "admin123", adresse);
        mvc.perform(get("/utilisateurs/admin").with(ip(adresse)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilisateur").value("admin"));
    }

    @Test
    @DisplayName("Sans token sur /utilisateurs/user -> 401")
    void sansToken() throws Exception {
        mvc.perform(get("/utilisateurs/user").with(ip(nouvelleIp())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token falsifie -> 401")
    void tokenFalsifie() throws Exception {
        String adresse = nouvelleIp();
        String token = login("user", "user123", adresse);
        String falsifie = token.substring(0, token.length() - 3) + "abc";
        mvc.perform(get("/utilisateurs/user").with(ip(adresse)).header("Authorization", "Bearer " + falsifie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token JWT invalide ou expire"));
    }

    @Test
    @DisplayName("Mauvais mot de passe -> 401")
    void mauvaisMotDePasse() throws Exception {
        mvc.perform(post("/auth/login").with(ip(nouvelleIp()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"faux\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    // Exercice 3 : XSS et validation

    @Test
    @DisplayName("Description contenant <script> -> neutralisee")
    void xssNeutralise() throws Exception {
        String adresse = nouvelleIp();
        String token = login("admin", "admin123", adresse);
        mvc.perform(post("/utilisateurs").with(ip(adresse))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Eve\",\"email\":\"eve@example.com\","
                                + "\"description\":\"<script>alert('xss')</script>\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("&lt;script&gt;alert('xss')&lt;/script&gt;"))
                .andExpect(jsonPath("$.description").value(not(containsString("<script>"))));
    }

    @Test
    @DisplayName("Email invalide -> 400 au format JSON uniforme")
    void emailInvalide() throws Exception {
        String adresse = nouvelleIp();
        String token = login("admin", "admin123", adresse);
        mvc.perform(post("/utilisateurs").with(ip(adresse))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Eve\",\"email\":\"pas-un-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Email invalide")));
    }

    @Test
    @DisplayName("Email deja utilise -> 409")
    void emailDuplique() throws Exception {
        String adresse = nouvelleIp();
        String token = login("admin", "admin123", adresse);
        String json = "{\"nom\":\"Dup\",\"email\":\"dup@example.com\"}";
        mvc.perform(post("/utilisateurs").with(ip(adresse)).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mvc.perform(post("/utilisateurs").with(ip(adresse)).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Tentative d'injection SQL dans la recherche -> traitee comme du texte")
    void injectionSqlNeutralisee() throws Exception {
        String adresse = nouvelleIp();
        String token = login("user", "user123", adresse);
        // si la requete etait concatenee ca renverrait tous les utilisateurs
        mvc.perform(get("/utilisateurs/recherche").with(ip(adresse))
                        .header("Authorization", "Bearer " + token)
                        .param("nom", "' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/utilisateurs/recherche").with(ip(adresse))
                        .header("Authorization", "Bearer " + token)
                        .param("nom", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }

    // Bonus : documentation OpenAPI

    @Test
    @DisplayName("Documentation OpenAPI accessible sans authentification")
    void documentationPublique() throws Exception {
        mvc.perform(get("/v3/api-docs").with(ip(nouvelleIp())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.apiKey").exists());
    }

    // Exercice 4 : rate limiting (implementation Bucket4j par defaut)

    @Test
    @DisplayName("Plus de 5 requetes en moins d'une minute -> 429")
    void rateLimit() throws Exception {
        String adresse = nouvelleIp();
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/utilisateurs").with(ip(adresse)).header("Authorization", "ApiKey 123456abcdef"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Rate-Limit-Remaining", String.valueOf(4 - i)));
        }
        mvc.perform(get("/api/utilisateurs").with(ip(adresse)).header("Authorization", "ApiKey 123456abcdef"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));

        // une autre IP n'est pas bloquee
        mvc.perform(get("/api/utilisateurs").with(ip(nouvelleIp())).header("Authorization", "ApiKey 123456abcdef"))
                .andExpect(status().isOk());
    }
}
