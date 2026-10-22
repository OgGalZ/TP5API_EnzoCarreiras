package com.example.tp.repository;

import com.example.tp.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    boolean existsByEmail(String email);

    // requete parametree (:nom) -> pas d'injection SQL possible, contrairement a une concatenation
    @Query("SELECT u FROM Utilisateur u WHERE LOWER(u.nom) LIKE LOWER(CONCAT('%', :nom, '%'))")
    List<Utilisateur> rechercherParNom(@Param("nom") String nom);
}
