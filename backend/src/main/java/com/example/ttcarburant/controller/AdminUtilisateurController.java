package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.UtilisateurDto;
import com.example.ttcarburant.security.JwtService;
import com.example.ttcarburant.services.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/utilisateurs")

@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class AdminUtilisateurController {

    private final UtilisateurService utilisateurService;

    public AdminUtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }
    /**
     * Récupérer tous les utilisateurs
     */
    @GetMapping
    public ResponseEntity<List<UtilisateurDto>> getAllUtilisateurs() {
        return ResponseEntity.ok(utilisateurService.getAllUtilisateurs());
    }

    /**
     * Récupérer les utilisateurs en attente de validation
     */
    @GetMapping("/en-attente")
    public ResponseEntity<List<UtilisateurDto>> getUtilisateursEnAttente() {
        return ResponseEntity.ok(utilisateurService.getUtilisateursEnAttente());
    }

    /**
     * Récupérer un utilisateur par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUtilisateurById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(utilisateurService.getUtilisateurById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Valider un compte utilisateur
     */
    @PatchMapping("/{id}/valider")
    public ResponseEntity<?> validerCompte(@PathVariable Long id) {
        try {
            UtilisateurDto utilisateur = utilisateurService.validerCompte(id);
            return ResponseEntity.ok(new SuccessResponse(
                    "Compte validé avec succès",
                    utilisateur
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Refuser un compte utilisateur
     */
    @PatchMapping("/{id}/refuser")
    public ResponseEntity<?> refuserCompte(@PathVariable Long id) {
        try {
            UtilisateurDto utilisateur = utilisateurService.refuserCompte(id);
            return ResponseEntity.ok(new SuccessResponse(
                    "Compte refusé",
                    utilisateur
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Désactiver un compte utilisateur
     */
    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<?> desactiverCompte(@PathVariable Long id) {
        try {
            UtilisateurDto utilisateur = utilisateurService.desactiverCompte(id);
            return ResponseEntity.ok(new SuccessResponse(
                    "Compte désactivé",
                    utilisateur
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Supprimer un utilisateur
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerUtilisateur(@PathVariable Long id) {
        try {
            utilisateurService.supprimerUtilisateur(id);
            return ResponseEntity.ok(new MessageResponse("Utilisateur supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Classes internes pour les réponses
     */
    private record ErrorResponse(String message) {}
    private record MessageResponse(String message) {}
    private record SuccessResponse(String message, UtilisateurDto data) {}
}