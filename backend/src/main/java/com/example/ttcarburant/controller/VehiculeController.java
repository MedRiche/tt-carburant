package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.VehiculeDto;
import com.example.ttcarburant.dto.VehiculeRequest;
import com.example.ttcarburant.services.VehiculeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicules")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class VehiculeController {

    private final VehiculeService vehiculeService;

    public VehiculeController(VehiculeService vehiculeService) {
        this.vehiculeService = vehiculeService;
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

    // ✅ FIX: explicit name in @PathVariable + :.+ to allow "/" in matricule
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

    // ✅ FIX: explicit name in @PathVariable + :.+ to allow "/" in matricule
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

    // ✅ FIX: explicit name in @PathVariable + :.+ to allow "/" in matricule
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

    private record ErrorResponse(String message) {}
    private record MessageResponse(String message) {}
    private record SuccessResponse(String message, VehiculeDto data) {}
}