package com.example.tp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// meme test mais avec le rate limiter maison de l'exercice 4
// (base H2 separee pour ne pas casser les donnees des autres tests)
@SpringBootTest(properties = {
        "app.security.rate-limit.impl=maison",
        "spring.datasource.url=jdbc:h2:mem:tpdb-maison;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class RateLimitMaisonTests {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Implementation maison : 6e requete en moins d'une minute -> 429")
    void rateLimitMaison() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/utilisateurs").with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                            .header("Authorization", "ApiKey 123456abcdef"))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/utilisateurs").with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                        .header("Authorization", "ApiKey 123456abcdef"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
