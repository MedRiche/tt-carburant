
package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.VehiculeDto;
import com.example.ttcarburant.dto.VehiculeRequest;
import com.example.ttcarburant.services.VehiculeService;
import com.example.ttcarburant.services.VehiculeImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/vehicules")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class VehiculeController {

    private final VehiculeService vehiculeService;
    private final VehiculeImportService importService;

    public VehiculeController(VehiculeService vehiculeService,
                              VehiculeImportService importService) {
        this.vehiculeService = vehiculeService;
        this.importService   = importService;
    }

    @GetMapping
    public ResponseEntity<List<VehiculeDto>> getAllVehicules() {
        return ResponseEntity.ok(vehiculeService.getAllVehicules());
    }

    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<List<VehiculeDto>> getVehiculesByZone(
            @PathVariable("zoneId") Long zoneId) {
        return ResponseEntity.ok(vehiculeService.getVehiculesByZone(zoneId));
    }

    @GetMapping("/{matricule:.+}")
    public ResponseEntity<?> getVehiculeById(
            @PathVariable("matricule") String matricule) {
        try {
            return ResponseEntity.ok(vehiculeService.getVehiculeById(matricule));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> creerVehicule(@Valid @RequestBody VehiculeRequest request) {
        try {
            VehiculeDto dto = vehiculeService.creerVehicule(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SuccessResponse("Véhicule créé avec succès", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{matricule:.+}")
    public ResponseEntity<?> modifierVehicule(
            @PathVariable("matricule") String matricule,
            @Valid @RequestBody VehiculeRequest request) {
        try {
            VehiculeDto dto = vehiculeService.modifierVehicule(matricule, request);
            return ResponseEntity.ok(new SuccessResponse("Véhicule modifié avec succès", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{matricule:.+}")
    public ResponseEntity<?> supprimerVehicule(
            @PathVariable("matricule") String matricule) {
        try {
            vehiculeService.supprimerVehicule(matricule);
            return ResponseEntity.ok(new MessageResponse("Véhicule supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/{matricule:.+}/zone/{zoneId}")
    public ResponseEntity<?> affecterZone(
            @PathVariable("matricule") String matricule,
            @PathVariable("zoneId") Long zoneId) {
        try {
            VehiculeDto dto = vehiculeService.affecterZone(matricule, zoneId);
            return ResponseEntity.ok(new SuccessResponse("Zone affectée avec succès", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── IMPORT EXCEL ──────────────────────────────────────────────────────────

    /**
     * Importe les véhicules depuis un fichier Excel (format DAF 2026).
     * @param file    fichier .xlsx
     * @param zoneNom nom de la zone à affecter (optionnel)
     */
    @PostMapping("/import")
    public ResponseEntity<?> importerExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "zoneNom", required = false) String zoneNom) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Le fichier est vide."));
            }
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Seuls les fichiers .xlsx sont acceptés."));
            }
            Map<String, Object> result = importService.importerVehicules(file, zoneNom);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur import : " + e.getMessage()));
        }
    }

    private record ErrorResponse(String message) {}
    private record MessageResponse(String message) {}
    private record SuccessResponse(String message, VehiculeDto data) {}
}