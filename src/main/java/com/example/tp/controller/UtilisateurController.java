package com.example.tp.controller;

import com.example.tp.model.Utilisateur;
import com.example.tp.model.UtilisateurRequest;
import com.example.tp.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// routes protegees par JWT (exercices 2 et 3), les droits sont dans SecurityConfig
@RestController
@RequestMapping("/utilisateurs")
@Tag(name = "Utilisateurs - JWT")
@SecurityRequirement(name = "bearerAuth")
public class UtilisateurController {

    private final UtilisateurService service;

    public UtilisateurController(UtilisateurService service) {
        this.service = service;
    }

    @Operation(summary = "Accessible a tout utilisateur connecte")
    @GetMapping("/user")
    public Map<String, Object> user(Authentication auth) {
        return Map.of(
                "message", "Espace utilisateur",
                "utilisateur", auth.getName(),
                "roles", auth.getAuthorities().toString());
    }

    @Operation(summary = "Reserve au role admin")
    @GetMapping("/admin")
    public Map<String, Object> admin(Authentication auth) {
        return Map.of(
                "message", "Espace administrateur",
                "utilisateur", auth.getName(),
                "nombreUtilisateurs", service.lister().size());
    }

    @Operation(summary = "Creer un utilisateur (admin)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Utilisateur creer(@Valid @RequestBody UtilisateurRequest request) {
        return service.creer(request);
    }

    @GetMapping
    public List<Utilisateur> lister() {
        return service.lister();
    }

    @Operation(summary = "Recherche par nom (requete parametree)")
    @GetMapping("/recherche")
    public List<Utilisateur> rechercher(@RequestParam @NotBlank @Size(max = 100) String nom) {
        return service.rechercherParNom(nom);
    }

    @GetMapping("/{id}")
    public Utilisateur trouver(@PathVariable Long id) {
        return service.trouver(id);
    }
}
