package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.Maintenance.MaintenanceDto;
import com.example.ttcarburant.dto.Maintenance.MaintenanceRequest;
import com.example.ttcarburant.dto.Maintenance.DetailMaintenanceDto;
import com.example.ttcarburant.model.enums.StatutMaintenance;
import com.example.ttcarburant.model.enums.TypeIntervention;
import com.example.ttcarburant.services.MaintenanceService;
import com.example.ttcarburant.services.MaintenanceImportService;
import com.example.ttcarburant.services.MaintenanceExportService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/maintenances")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class AdminMaintenanceController {

    private final MaintenanceService maintenanceService;
    private final MaintenanceImportService importService;
    private final MaintenanceExportService exportService;

    public AdminMaintenanceController(MaintenanceService maintenanceService,
                                      MaintenanceImportService importService,
                                      MaintenanceExportService exportService) {
        this.maintenanceService = maintenanceService;
        this.importService = importService;
        this.exportService = exportService;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> creer(@Valid @RequestBody MaintenanceRequest req) {
        try {
            MaintenanceDto dto = maintenanceService.creer(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SuccessResponse("Dossier créé avec succès", dto));
        } catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceDto>> getAll() {
        return ResponseEntity.ok(maintenanceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try { return ResponseEntity.ok(maintenanceService.getById(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@PathVariable Long id,
                                      @Valid @RequestBody MaintenanceRequest req) {
        try { return ResponseEntity.ok(new SuccessResponse("Dossier modifié", maintenanceService.modifier(id, req))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        try { maintenanceService.supprimer(id); return ResponseEntity.ok(new MsgResponse("Dossier supprimé")); }
        catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    // ── Filtres ───────────────────────────────────────────────────────────

    @GetMapping("/vehicule/{matricule:.+}")
    public ResponseEntity<List<MaintenanceDto>> getByVehicule(@PathVariable String matricule) {
        return ResponseEntity.ok(maintenanceService.getByVehicule(matricule));
    }

    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<List<MaintenanceDto>> getByZone(@PathVariable Long zoneId) {
        return ResponseEntity.ok(maintenanceService.getByZone(zoneId));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<MaintenanceDto>> getByStatut(@PathVariable StatutMaintenance statut) {
        return ResponseEntity.ok(maintenanceService.getByStatut(statut));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<MaintenanceDto>> getByType(@PathVariable TypeIntervention type) {
        return ResponseEntity.ok(maintenanceService.getByType(type));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MaintenanceDto>> search(@RequestParam String q) {
        return ResponseEntity.ok(maintenanceService.search(q));
    }

    // ── Global Vehicle List (vue principale) ─────────────────────────────

    @GetMapping("/global-list")
    public ResponseEntity<List<Map<String, Object>>> getGlobalVehicleList() {
        return ResponseEntity.ok(maintenanceService.getGlobalVehicleList());
    }

    // ── Gestion des détails ───────────────────────────────────────────────

    @PostMapping("/{id}/details")
    public ResponseEntity<?> ajouterDetail(@PathVariable Long id,
                                           @RequestBody DetailMaintenanceDto req) {
        try { return ResponseEntity.ok(new SuccessResponse("Détail ajouté", maintenanceService.ajouterDetail(id, req))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    @DeleteMapping("/{maintenanceId}/details/{detailId}")
    public ResponseEntity<?> supprimerDetail(@PathVariable Long maintenanceId,
                                             @PathVariable Long detailId) {
        try { return ResponseEntity.ok(new SuccessResponse("Détail supprimé", maintenanceService.supprimerDetail(maintenanceId, detailId))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    // ── Analytics ─────────────────────────────────────────────────────────

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(maintenanceService.getDashboard());
    }

    // ── Import Dataset_Complet.xlsx ───────────────────────────────────────

    @PostMapping("/import")
    public ResponseEntity<?> importerDataset(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = importService.importerDataset(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err(e));
        }
    }

    // ── Export Excel ──────────────────────────────────────────────────────

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exporterExcel(
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String statut) {
        try {
            byte[] bytes = exportService.exporterExcel(zoneId, statut);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=maintenances.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Records ───────────────────────────────────────────────────────────
    private record ErrResponse(String message) {}
    private record MsgResponse(String message) {}
    private record SuccessResponse(String message, MaintenanceDto data) {}
    private ErrResponse err(Exception e) { return new ErrResponse(e.getMessage()); }
}