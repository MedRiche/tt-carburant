package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.CarburantVehiculeDto;
import com.example.ttcarburant.dto.CarburantVehiculeRequest;
import com.example.ttcarburant.services.CarburantVehiculeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    public CarburantVehiculeController(CarburantVehiculeService service) {
        this.service = service;
    }

    // ✅ FIX: explicit name in @PathVariable + :.+ for matricules with "/"
    @GetMapping("/vehicule/{matricule:.+}")
    public ResponseEntity<List<CarburantVehiculeDto>> getByVehicule(
            @PathVariable("matricule") String matricule) {
        return ResponseEntity.ok(service.getByVehicule(matricule));
    }

    @GetMapping("/periode")
    public ResponseEntity<List<CarburantVehiculeDto>> getByPeriode(
            @RequestParam("annee") int annee,
            @RequestParam("mois") int mois) {
        return ResponseEntity.ok(service.getByPeriode(annee, mois));
    }

    // ✅ FIX: explicit name in @PathVariable
    @GetMapping("/zone/{zoneId}/periode")
    public ResponseEntity<List<CarburantVehiculeDto>> getByZone(
            @PathVariable("zoneId") Long zoneId,
            @RequestParam("annee") int annee,
            @RequestParam("mois") int mois) {
        return ResponseEntity.ok(service.getByZoneAndPeriode(zoneId, annee, mois));
    }

    // ✅ FIX: explicit name in @PathVariable
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) {
        try { return ResponseEntity.ok(service.getById(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    @PostMapping
    public ResponseEntity<?> saisir(@Valid @RequestBody CarburantVehiculeRequest req) {
        try {
            CarburantVehiculeDto dto = service.saisir(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SuccessResponse("Saisie enregistrée", dto));
        } catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    // ✅ FIX: explicit name in @PathVariable
    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(
            @PathVariable("id") Long id,
            @Valid @RequestBody CarburantVehiculeRequest req) {
        try {
            CarburantVehiculeDto dto = service.modifier(id, req);
            return ResponseEntity.ok(new SuccessResponse("Saisie modifiée", dto));
        } catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    // ✅ FIX: explicit name in @PathVariable
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@PathVariable("id") Long id) {
        try { service.supprimer(id); return ResponseEntity.ok(new MsgResponse("Supprimé")); }
        catch (Exception e) { return ResponseEntity.badRequest().body(err(e)); }
    }

    private record ErrResponse(String message) {}
    private record MsgResponse(String message) {}
    private record SuccessResponse(String message, CarburantVehiculeDto data) {}
    private ErrResponse err(Exception e) { return new ErrResponse(e.getMessage()); }
}