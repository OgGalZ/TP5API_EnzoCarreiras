package com.example.tp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "utilisateurs")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // plus long que la limite du DTO car l'echappement HTML rallonge le texte
    @Column(nullable = false, length = 600)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 3000)
    private String description;

    public Utilisateur() {
    }

    public Utilisateur(String nom, String email, String description) {
        this.nom = nom;
        this.email = email;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
