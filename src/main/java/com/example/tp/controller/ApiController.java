package com.example.tp.controller;

import com.example.tp.model.Utilisateur;
import com.example.tp.service.UtilisateurService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// routes protegees par la cle API (exercice 1)
@RestController
@RequestMapping("/api")
@Tag(name = "API - cle API")
@SecurityRequirement(name = "apiKey")
public class ApiController {

    private final UtilisateurService service;

    public ApiController(UtilisateurService service) {
        this.service = service;
    }

    @GetMapping("/utilisateurs")
    public List<Utilisateur> lister() {
        return service.lister();
    }
}
