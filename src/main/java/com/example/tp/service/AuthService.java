package com.example.tp.service;

import com.example.tp.model.CompteUtilisateur;
import com.example.tp.model.LoginResponse;
import com.example.tp.repository.CompteUtilisateurRepository;
import com.example.tp.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final CompteUtilisateurRepository comptes;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final String hashBidon;

    public AuthService(CompteUtilisateurRepository comptes, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.comptes = comptes;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.hashBidon = passwordEncoder.encode("bidon");
    }

    public LoginResponse login(String username, String password) {
        CompteUtilisateur compte = comptes.findByUsername(username).orElse(null);

        // on fait le BCrypt meme si le compte n'existe pas, pour que le temps de reponse
        // ne permette pas de savoir si un username existe
        String hash = (compte != null) ? compte.getPasswordHash() : hashBidon;
        boolean ok = passwordEncoder.matches(password, hash);

        if (compte == null || !ok) {
            throw new BadCredentialsException("Identifiants invalides");
        }

        String token = jwtUtil.generateToken(compte.getUsername(), compte.getRole());
        return new LoginResponse(token, "Bearer", jwtUtil.getExpirationSecondes(), compte.getRole());
    }
}
