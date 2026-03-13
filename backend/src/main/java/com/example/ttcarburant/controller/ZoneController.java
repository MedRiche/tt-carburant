package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.ZoneDto;
import com.example.ttcarburant.dto.ZoneRequest;
import com.example.ttcarburant.services.ZoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/zones")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    /**
     * Créer une nouvelle zone
     */
    @PostMapping
    public ResponseEntity<?> creerZone(@Valid @RequestBody ZoneRequest request) {
        try {
            ZoneDto zone = zoneService.creerZone(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new SuccessResponse("Zone créée avec succès", zone)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Récupérer toutes les zones
     */
    @GetMapping
    public ResponseEntity<List<ZoneDto>> getAllZones() {
        return ResponseEntity.ok(zoneService.getAllZones());
    }

    /**
     * Récupérer une zone par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getZoneById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(zoneService.getZoneById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Modifier une zone
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> modifierZone(
            @PathVariable Long id,
            @Valid @RequestBody ZoneRequest request) {
        try {
            ZoneDto zone = zoneService.modifierZone(id, request);
            return ResponseEntity.ok(new SuccessResponse("Zone modifiée avec succès", zone));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Supprimer une zone
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerZone(@PathVariable Long id) {
        try {
            zoneService.supprimerZone(id);
            return ResponseEntity.ok(new MessageResponse("Zone supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Classes internes pour les réponses
     */
    private record ErrorResponse(String message) {}
    private record MessageResponse(String message) {}
    private record SuccessResponse(String message, ZoneDto data) {}
}