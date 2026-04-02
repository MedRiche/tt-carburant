package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.HistoriqueModificationDto;
import com.example.ttcarburant.services.HistoriqueModificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/historique-carburant")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class HistoriqueModificationCarburantController {

    private final HistoriqueModificationService historiqueService;

    public HistoriqueModificationCarburantController(HistoriqueModificationService svc) {
        this.historiqueService = svc;
    }

    /** Historique global (200 derniers) */
    @GetMapping
    public ResponseEntity<List<HistoriqueModificationDto>> getTout() {
        return ResponseEntity.ok(historiqueService.getToutHistorique());
    }

    /** Historique d'un véhicule (optionnel: filtrer par année) */
    @GetMapping("/vehicule/{matricule}")
    public ResponseEntity<List<HistoriqueModificationDto>> getParVehicule(
            @PathVariable String matricule,
            @RequestParam(required = false) Integer annee) {
        if (annee != null)
            return ResponseEntity.ok(historiqueService.getHistoriqueParVehiculeEtAnnee(matricule, annee));
        return ResponseEntity.ok(historiqueService.getHistoriqueParVehicule(matricule));
    }

    /** Historique d'un mois/année */
    @GetMapping("/periode")
    public ResponseEntity<List<HistoriqueModificationDto>> getParPeriode(
            @RequestParam int annee,
            @RequestParam int mois) {
        return ResponseEntity.ok(historiqueService.getHistoriqueParPeriode(annee, mois));
    }

    /** Historique d'une saisie spécifique (par ID) */
    @GetMapping("/saisie/{id}")
    public ResponseEntity<List<HistoriqueModificationDto>> getParSaisie(
            @PathVariable Long id) {
        return ResponseEntity.ok(historiqueService.getHistoriqueParGestionId(id));
    }
}