// ══════════════════════════════════════════════════════════════════
// FILE 1: VerrouillageCarburantController.java
// ══════════════════════════════════════════════════════════════════
package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.VerrouillageDto;
import com.example.ttcarburant.services.VerrouillageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/verrouillage-carburant")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class VerrouillageCarburantController {

    private final VerrouillageService verrouillageService;

    public VerrouillageCarburantController(VerrouillageService verrouillageService) {
        this.verrouillageService = verrouillageService;
    }

    /** Vérifier le statut de verrouillage d'un mois */
    @GetMapping("/statut")
    public ResponseEntity<VerrouillageDto> getStatut(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Long zoneId) {
        return ResponseEntity.ok(verrouillageService.getStatut(annee, mois, zoneId));
    }

    /** Vérifier si verrouillé (endpoint simple) */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> isVerrouille(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Long zoneId) {
        boolean verr = verrouillageService.isVerrouille(annee, mois, zoneId);
        return ResponseEntity.ok(Map.of("verrouille", verr));
    }

    /** Verrouiller un mois */
    @PostMapping("/verrouiller")
    public ResponseEntity<?> verrouiller(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Long zoneId) {
        try {
            VerrouillageDto dto = verrouillageService.verrouiller(annee, mois, zoneId);
            return ResponseEntity.ok(new SuccessResponse("Mois verrouillé avec succès", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /** Déverrouiller un mois */
    @PostMapping("/deverrouiller")
    public ResponseEntity<?> deverrouiller(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Long zoneId) {
        try {
            VerrouillageDto dto = verrouillageService.deverrouiller(annee, mois, zoneId);
            return ResponseEntity.ok(new SuccessResponse("Mois déverrouillé avec succès", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /** Lister tous les verrous d'une année */
    @GetMapping("/annee/{annee}")
    public ResponseEntity<List<VerrouillageDto>> getVerrouxParAnnee(@PathVariable int annee) {
        return ResponseEntity.ok(verrouillageService.getVerrouxParAnnee(annee));
    }

    /** Tous les mois actuellement verrouillés */
    @GetMapping("/actifs")
    public ResponseEntity<List<VerrouillageDto>> getTousLesVerrouxActifs() {
        return ResponseEntity.ok(verrouillageService.getTousLesVerrouxActifs());
    }

    private record ErrorResponse(String message) {}
    private record SuccessResponse(String message, VerrouillageDto data) {}
}

