package com.example.tp.service;

import com.example.tp.exception.ConflitException;
import com.example.tp.exception.RessourceIntrouvableException;
import com.example.tp.model.Utilisateur;
import com.example.tp.model.UtilisateurRequest;
import com.example.tp.repository.UtilisateurRepository;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtilisateurService {

    private final UtilisateurRepository repository;

    public UtilisateurService(UtilisateurRepository repository) {
        this.repository = repository;
    }

    public Utilisateur creer(UtilisateurRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (repository.existsByEmail(email)) {
            throw new ConflitException("Cet email est deja utilise");
        }

        // Exercice 3 : on echappe le HTML avant de stocker (<script> devient &lt;script&gt;)
        String nom = StringEscapeUtils.escapeHtml4(request.getNom().trim());
        String description = null;
        if (request.getDescription() != null) {
            description = StringEscapeUtils.escapeHtml4(request.getDescription());
        }

        return repository.save(new Utilisateur(nom, email, description));
    }

    public List<Utilisateur> lister() {
        return repository.findAll();
    }

    public Utilisateur trouver(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur " + id + " introuvable"));
    }

    public List<Utilisateur> rechercherParNom(String nom) {
        // les noms sont stockes echappes, donc on echappe aussi la recherche
        return repository.rechercherParNom(StringEscapeUtils.escapeHtml4(nom));
    }
}
