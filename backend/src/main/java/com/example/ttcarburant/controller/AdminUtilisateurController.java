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

    // ── GET ALL ──────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<UtilisateurDto>> getAllUtilisateurs() {
        return ResponseEntity.ok(utilisateurService.getAllUtilisateurs());
    }

    /** Tous les comptes EN_ATTENTE (techniciens normaux + conducteurs importés) */
    @GetMapping("/en-attente")
    public ResponseEntity<List<UtilisateurDto>> getUtilisateursEnAttente() {
        return ResponseEntity.ok(utilisateurService.getUtilisateursEnAttente());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUtilisateurById(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(utilisateurService.getUtilisateurById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── VALIDATION avec affectation de zones ──────────────────────────────────

    /**
     * Valide un compte EN_ATTENTE et lui affecte des zones.
     * Fonctionne pour tout TECHNICIEN (conducteur ou non).
     */
    @PostMapping("/valider")
    public ResponseEntity<?> validerCompteAvecZones(@Valid @RequestBody ValiderCompteRequest request) {
        try {
            UtilisateurDto utilisateur = utilisateurService.validerCompteAvecZones(request);
            return ResponseEntity.ok(new SuccessResponse(
                    "Compte validé et zones affectées avec succès", utilisateur));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── REFUS ─────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/refuser")
    public ResponseEntity<?> refuserCompte(@PathVariable("id") Long id) {
        try {
            UtilisateurDto utilisateur = utilisateurService.refuserCompte(id);
            return ResponseEntity.ok(new SuccessResponse("Compte refusé", utilisateur));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── TOGGLE ACTIVATION ─────────────────────────────────────────────────────

    @PatchMapping("/{id}/toggle-activation")
    public ResponseEntity<?> toggleActivation(@PathVariable("id") Long id) {
        try {
            UtilisateurDto utilisateur = utilisateurService.toggleActivation(id);
            String msg = utilisateur.getStatutCompte().name().equals("ACTIF")
                    ? "Compte activé avec succès"
                    : "Compte désactivé avec succès";
            return ResponseEntity.ok(new SuccessResponse(msg, utilisateur));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── SUPPRESSION ───────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerUtilisateur(@PathVariable("id") Long id) {
        try {
            utilisateurService.supprimerUtilisateur(id);
            return ResponseEntity.ok(new MessageResponse("Utilisateur supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── GESTION DES ZONES ─────────────────────────────────────────────────────

    @PostMapping("/{utilisateurId}/zones/{zoneId}")
    public ResponseEntity<?> ajouterZone(
            @PathVariable("utilisateurId") Long utilisateurId,
            @PathVariable("zoneId") Long zoneId) {
        try {
            UtilisateurDto utilisateur = utilisateurService.ajouterZone(utilisateurId, zoneId);
            return ResponseEntity.ok(new SuccessResponse("Zone ajoutée avec succès", utilisateur));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{utilisateurId}/zones/{zoneId}")
    public ResponseEntity<?> retirerZone(
            @PathVariable("utilisateurId") Long utilisateurId,
            @PathVariable("zoneId") Long zoneId) {
        try {
            UtilisateurDto utilisateur = utilisateurService.retirerZone(utilisateurId, zoneId);
            return ResponseEntity.ok(new SuccessResponse("Zone retirée avec succès", utilisateur));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Records internes ──────────────────────────────────────────────────────

    private record ErrorResponse(String message) {}
    private record MessageResponse(String message) {}
    private record SuccessResponse(String message, UtilisateurDto data) {}
}