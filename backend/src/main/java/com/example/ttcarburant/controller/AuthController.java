package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.AuthResponse;
import com.example.ttcarburant.dto.LoginRequest;
import com.example.ttcarburant.dto.RegisterRequest;
import com.example.ttcarburant.services.AuthService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")

@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    /**
     * Inscription d'un nouvel utilisateur
     * Le compte sera créé avec le statut EN_ATTENTE
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Connexion d'un utilisateur
     * Seuls les comptes avec statut ACTIF peuvent se connecter
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Classe interne pour les réponses d'erreur
     */
    private record ErrorResponse(String message) {}
}