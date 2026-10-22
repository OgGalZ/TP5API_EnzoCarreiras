package com.example.tp.repository;

import com.example.tp.model.CompteUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompteUtilisateurRepository extends JpaRepository<CompteUtilisateur, Long> {

    Optional<CompteUtilisateur> findByUsername(String username);

    boolean existsByUsername(String username);
}
