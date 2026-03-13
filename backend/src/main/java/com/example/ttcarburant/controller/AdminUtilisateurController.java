package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.UtilisateurDto;
import com.example.ttcarburant.dto.ValiderCompteRequest;
import com.example.ttcarburant.services.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * Valider un compte utilisateur AVEC affectation de zones
     */
    @PostMapping("/valider")
    public ResponseEntity<?> validerCompteAvecZones(@Valid @RequestBody ValiderCompteRequest request) {
        try {
            UtilisateurDto utilisateur = utilisateurService.validerCompteAvecZones(request);
            return ResponseEntity.ok(new SuccessResponse(
                    "Compte validé et zones affectées avec succès",
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
     * Ajouter une zone à un utilisateur
     */
    @PostMapping("/{utilisateurId}/zones/{zoneId}")
    public ResponseEntity<?> ajouterZone(
            @PathVariable Long utilisateurId,
            @PathVariable Long zoneId) {
        try {
            UtilisateurDto utilisateur = utilisateurService.ajouterZone(utilisateurId, zoneId);
            return ResponseEntity.ok(new SuccessResponse(
                    "Zone ajoutée avec succès",
                    utilisateur
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Retirer une zone d'un utilisateur
     */
    @DeleteMapping("/{utilisateurId}/zones/{zoneId}")
    public ResponseEntity<?> retirerZone(
            @PathVariable Long utilisateurId,
            @PathVariable Long zoneId) {
        try {
            UtilisateurDto utilisateur = utilisateurService.retirerZone(utilisateurId, zoneId);
            return ResponseEntity.ok(new SuccessResponse(
                    "Zone retirée avec succès",
                    utilisateur
            ));
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