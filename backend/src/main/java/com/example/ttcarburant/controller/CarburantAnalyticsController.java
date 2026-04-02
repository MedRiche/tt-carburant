package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.analytics.*;
import com.example.ttcarburant.services.CarburantAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carburant-analytics")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class CarburantAnalyticsController {

    private final CarburantAnalyticsService analyticsService;

    public CarburantAnalyticsController(CarburantAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // ── 1. HISTORIQUE RAVITAILLEMENT DÉTAILLÉ ─────────────────────────────────

    @GetMapping("/historique/{matricule}")
    public ResponseEntity<List<HistoriqueRavitaillementDto>> getHistorique(
            @PathVariable String matricule,
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(analyticsService.getHistoriqueRavitaillement(matricule, annee));
    }

    @GetMapping("/historique/zone/{zoneId}")
    public ResponseEntity<List<HistoriqueRavitaillementDto>> getHistoriqueZone(
            @PathVariable Long zoneId,
            @RequestParam int annee) {
        return ResponseEntity.ok(analyticsService.getHistoriqueZone(zoneId, annee));
    }

    // ── 2. GRAPHIQUES D'ÉVOLUTION ─────────────────────────────────────────────

    @GetMapping("/evolution/{matricule}")
    public ResponseEntity<EvolutionDto> getEvolution(
            @PathVariable String matricule,
            @RequestParam int annee) {
        return ResponseEntity.ok(analyticsService.getEvolutionVehicule(matricule, annee));
    }

    @GetMapping("/evolution/zone/{zoneId}")
    public ResponseEntity<EvolutionDto> getEvolutionZone(
            @PathVariable Long zoneId,
            @RequestParam int annee) {
        return ResponseEntity.ok(analyticsService.getEvolutionZone(zoneId, annee));
    }

    // ── 3. DÉTECTION D'ANOMALIES ──────────────────────────────────────────────

    @GetMapping("/anomalies")
    public ResponseEntity<List<AnomalieDto>> getAnomalies(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Long zoneId) {
        return ResponseEntity.ok(analyticsService.detecterAnomalies(annee, mois, zoneId));
    }

    @GetMapping("/anomalies/annee/{annee}")
    public ResponseEntity<List<AnomalieDto>> getAnomaliesAnnee(
            @PathVariable int annee,
            @RequestParam(required = false) Long zoneId) {
        return ResponseEntity.ok(analyticsService.detecterAnomaliesAnnee(annee, zoneId));
    }

    // ── 4. COMPARAISON ENTRE VÉHICULES ────────────────────────────────────────

    @GetMapping("/comparaison")
    public ResponseEntity<ComparaisonDto> getComparaison(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Long zoneId) {
        return ResponseEntity.ok(analyticsService.getComparaisonVehicules(annee, mois, zoneId));
    }

    @GetMapping("/comparaison/annuel")
    public ResponseEntity<ComparaisonDto> getComparaisonAnnuelle(
            @RequestParam int annee,
            @RequestParam(required = false) Long zoneId) {
        return ResponseEntity.ok(analyticsService.getComparaisonAnnuelle(annee, zoneId));
    }

    // ── 5. DASHBOARD AVANCÉ CARBURANT ─────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardCarburantDto> getDashboard(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Long zoneId) {
        return ResponseEntity.ok(analyticsService.getDashboardAvance(annee, mois, zoneId));
    }

    @GetMapping("/dashboard/annuel")
    public ResponseEntity<DashboardCarburantDto> getDashboardAnnuel(
            @RequestParam int annee,
            @RequestParam(required = false) Long zoneId) {
        return ResponseEntity.ok(analyticsService.getDashboardAnnuel(annee, zoneId));
    }
}