package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.CarburantVehiculeDto;
import com.example.ttcarburant.dto.CarburantVehiculeRequest;
import com.example.ttcarburant.services.CarburantVehiculeService;
import com.example.ttcarburant.services.CarburantExportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carburant-vehicules")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class CarburantVehiculeController {

    private final CarburantVehiculeService service;
    private final CarburantExportService exportService;

    public CarburantVehiculeController(CarburantVehiculeService service,
                                       CarburantExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    // ── Lecture ──────────────────────────────────────────────────

    @GetMapping("/vehicule/{matricule}")
    public ResponseEntity<List<CarburantVehiculeDto>> getByVehicule(@PathVariable String matricule) {
        return ResponseEntity.ok(service.getByVehicule(matricule));
    }

    @GetMapping("/periode")
    public ResponseEntity<List<CarburantVehiculeDto>> getByPeriode(
            @RequestParam int annee, @RequestParam int mois) {
        return ResponseEntity.ok(service.getByPeriode(annee, mois));
    }

    @GetMapping("/zone/{zoneId}/periode")
    public ResponseEntity<List<CarburantVehiculeDto>> getByZone(
            @PathVariable Long zoneId,
            @RequestParam int annee, @RequestParam int mois) {
        return ResponseEntity.ok(service.getByZoneAndPeriode(zoneId, annee, mois));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try { return ResponseEntity.ok(service.getById(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    // ── NOUVEAU : pré-remplissage règles 6 & 7 ───────────────────

    /**
     * Retourne les valeurs pré-remplies du mois précédent pour un véhicule donné.
     * Index démarrage (règle 6) = index fin du mois précédent
     * Montant restant (règle 7) = montant restant réservoir fin du mois précédent
     */
    @GetMapping("/prefill/{matricule}")
    public ResponseEntity<?> getPrefill(
            @PathVariable String matricule,
            @RequestParam int annee,
            @RequestParam int mois) {
        try {
            return ResponseEntity.ok(service.getPrefillFromPreviousMonth(matricule, annee, mois));
        } catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    // ── NOUVEAU : récapitulatif annuel ────────────────────────────

    @GetMapping("/recap/vehicule/{matricule}")
    public ResponseEntity<List<CarburantVehiculeDto>> getRecapAnnuelVehicule(
            @PathVariable String matricule,
            @RequestParam int annee) {
        return ResponseEntity.ok(service.getRecapAnnuelByVehicule(matricule, annee));
    }

    @GetMapping("/recap/zone/{zoneId}")
    public ResponseEntity<List<CarburantVehiculeDto>> getRecapAnnuelZone(
            @PathVariable Long zoneId,
            @RequestParam int annee) {
        return ResponseEntity.ok(service.getRecapAnnuelByZone(zoneId, annee));
    }

    // ── NOUVEAU : export Excel ────────────────────────────────────

    @GetMapping("/export/excel/mensuel")
    public ResponseEntity<byte[]> exportExcelMensuel(
            @RequestParam int annee,
            @RequestParam int mois,
            @RequestParam(required = false) Long zoneId) {
        try {
            byte[] bytes = exportService.exportMensuelExcel(annee, mois, zoneId);
            String filename = "carburant_" + annee + "_" + String.format("%02d", mois) + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/excel/annuel")
    public ResponseEntity<byte[]> exportExcelAnnuel(
            @RequestParam int annee,
            @RequestParam(required = false) Long zoneId) {
        try {
            byte[] bytes = exportService.exportAnnuelExcel(annee, zoneId);
            String filename = "carburant_annuel_" + annee + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── NOUVEAU : alertes budget dépassé ──────────────────────────

    @GetMapping("/alertes/budget")
    public ResponseEntity<List<CarburantVehiculeDto>> getBudgetDepasses(
            @RequestParam int annee, @RequestParam int mois) {
        return ResponseEntity.ok(service.getBudgetDepasses(annee, mois));
    }

    // ── CRUD ──────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> saisir(@Valid @RequestBody CarburantVehiculeRequest req) {
        try {
            CarburantVehiculeDto dto = service.saisir(req);
            // Alerte si budget dépassé
            if (dto.isBudgetDepasse()) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new AlertResponse(
                                "Saisie enregistrée — ⚠️ Budget dépassé de " + dto.getDepassementMontant() + " DT",
                                dto, true));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SuccessResponse("Saisie enregistrée", dto));
        } catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@PathVariable Long id,
                                      @Valid @RequestBody CarburantVehiculeRequest req) {
        try {
            CarburantVehiculeDto dto = service.modifier(id, req);
            if (dto.isBudgetDepasse()) {
                return ResponseEntity.ok(new AlertResponse(
                        "Saisie modifiée — ⚠️ Budget dépassé de " + dto.getDepassementMontant() + " DT",
                        dto, true));
            }
            return ResponseEntity.ok(new SuccessResponse("Saisie modifiée", dto));
        } catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        try { service.supprimer(id); return ResponseEntity.ok(new MsgResponse("Supprimé")); }
        catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    // ── Records internes ──
    private record ErrResponse(String message) {}
    private record MsgResponse(String message) {}
    private record SuccessResponse(String message, CarburantVehiculeDto data) {}
    private record AlertResponse(String message, CarburantVehiculeDto data, boolean alert) {}
    private ErrResponse err(Exception e) { return new ErrResponse(e.getMessage()); }
}