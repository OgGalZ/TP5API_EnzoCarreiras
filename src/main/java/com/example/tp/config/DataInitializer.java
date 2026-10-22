package com.example.tp.config;

import com.example.tp.model.CompteUtilisateur;
import com.example.tp.model.UtilisateurRequest;
import com.example.tp.repository.CompteUtilisateurRepository;
import com.example.tp.service.UtilisateurService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// cree les comptes et quelques utilisateurs au demarrage (la base H2 est en memoire)
@Component
public class DataInitializer implements CommandLineRunner {

    private final CompteUtilisateurRepository comptes;
    private final UtilisateurService utilisateurService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.user-password}")
    private String userPassword;

    public DataInitializer(CompteUtilisateurRepository comptes, UtilisateurService utilisateurService,
                           PasswordEncoder passwordEncoder) {
        this.comptes = comptes;
        this.utilisateurService = utilisateurService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!comptes.existsByUsername("admin")) {
            comptes.save(new CompteUtilisateur("admin", passwordEncoder.encode(adminPassword), "admin"));
        }
        if (!comptes.existsByUsername("user")) {
            comptes.save(new CompteUtilisateur("user", passwordEncoder.encode(userPassword), "user"));
        }

        if (utilisateurService.lister().isEmpty()) {
            utilisateurService.creer(utilisateur("Alice Martin", "alice@example.com", "Compte de test"));
            utilisateurService.creer(utilisateur("Bob Durand", "bob@example.com", "Compte de test"));
        }
    }

    private UtilisateurRequest utilisateur(String nom, String email, String description) {
        UtilisateurRequest u = new UtilisateurRequest();
        u.setNom(nom);
        u.setEmail(email);
        u.setDescription(description);
        return u;
    }
}
