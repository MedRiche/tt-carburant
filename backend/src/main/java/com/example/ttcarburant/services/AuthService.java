package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.AuthResponse;
import com.example.ttcarburant.dto.LoginRequest;
import com.example.ttcarburant.dto.RegisterRequest;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.enums.StatutCompte;
import com.example.ttcarburant.repository.UtilisateurRepository;
import com.example.ttcarburant.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        Utilisateur utilisateur = new Utilisateur();

        utilisateur.setNom(request.getNom());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setRole(request.getRole());
        utilisateur.setSpecialite(request.getSpecialite());
        utilisateur.setStatutCompte(StatutCompte.EN_ATTENTE);

        utilisateurRepository.save(utilisateur);

        AuthResponse response = new AuthResponse();
        response.setUserId(utilisateur.getId());
        response.setNom(utilisateur.getNom());
        response.setEmail(utilisateur.getEmail());
        response.setRole(utilisateur.getRole());
        response.setStatutCompte(utilisateur.getStatutCompte());
        response.setMessage("Inscription réussie ! Votre compte est en attente de validation par un administrateur.");

        return response;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        if (utilisateur.getStatutCompte() == StatutCompte.EN_ATTENTE) {
            throw new RuntimeException("Votre compte est en attente de validation");
        }

        if (utilisateur.getStatutCompte() == StatutCompte.REFUSE) {
            throw new RuntimeException("Votre compte a été refusé");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getMotDePasse()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        String jwtToken = jwtService.generateToken(utilisateur);

        AuthResponse response = new AuthResponse();
        response.setToken(jwtToken);
        response.setType("Bearer");
        response.setUserId(utilisateur.getId());
        response.setNom(utilisateur.getNom());
        response.setEmail(utilisateur.getEmail());
        response.setRole(utilisateur.getRole());
        response.setStatutCompte(utilisateur.getStatutCompte());
        response.setMessage("Connexion réussie");

        return response;
    }
}