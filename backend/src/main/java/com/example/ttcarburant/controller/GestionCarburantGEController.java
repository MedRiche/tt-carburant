package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.GroupeElectrogene.GestionCarburantGEDto;
import com.example.ttcarburant.dto.GroupeElectrogene.GestionCarburantGERequest;
import com.example.ttcarburant.model.enums.Semestre;
import com.example.ttcarburant.services.GestionCarburantGEService;
import com.example.ttcarburant.services.GroupeElectrogeneImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/carburant-ge")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class GestionCarburantGEController {

    private final GestionCarburantGEService service;
    private final GroupeElectrogeneImportService importService;

    public GestionCarburantGEController(GestionCarburantGEService service,
                                        GroupeElectrogeneImportService importService) {
        this.service = service;
        this.importService = importService;
    }

    /**
     * CORRECTION : endpoint GET / manquant.
     * Le frontend Angular appelle GET /api/admin/carburant-ge via getAllSaisies()
     * pour afficher l'historique dans la liste des groupes électrogènes.
     */
    @GetMapping
    public ResponseEntity<List<GestionCarburantGEDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<?> saisir(@Valid @RequestBody GestionCarburantGERequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SuccessResponse("Saisie enregistrée", service.saisir(req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@PathVariable Long id, @Valid @RequestBody GestionCarburantGERequest req) {
        try {
            return ResponseEntity.ok(new SuccessResponse("Saisie modifiée", service.modifier(id, req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        try {
            service.supprimer(id);
            return ResponseEntity.ok(new MessageResponse("Supprimé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/site/{site}")
    public ResponseEntity<List<GestionCarburantGEDto>> getBySite(@PathVariable String site) {
        return ResponseEntity.ok(service.getBySite(site));
    }

    @GetMapping("/periode")
    public ResponseEntity<List<GestionCarburantGEDto>> getByPeriode(@RequestParam int annee,
                                                                    @RequestParam Semestre semestre) {
        return ResponseEntity.ok(service.getByPeriode(annee, semestre));
    }

    @GetMapping("/zone/{zoneId}/periode")
    public ResponseEntity<List<GestionCarburantGEDto>> getByZone(@PathVariable Long zoneId,
                                                                 @RequestParam int annee,
                                                                 @RequestParam Semestre semestre) {
        return ResponseEntity.ok(service.getByZoneAndPeriode(zoneId, annee, semestre));
    }

    /**
     * CORRECTION : cet endpoint restait sous /carburant-ge/import
     * et est maintenant cohérent avec le service Angular (GroupeElectrogeneImportService).
     * Le service Angular a été corrigé pour pointer vers /groupes-electrogenes/import
     * qui est géré par GroupeElectrogeneController.
     */
    @PostMapping("/import")
    public ResponseEntity<?> importerExcel(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "zoneNom", required = false) String zoneNom) {
        try {
            Map<String, Object> result = importService.importerDepuisExcel(file, zoneNom);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    private record ErrorResponse(String message) {}
    private record MessageResponse(String message) {}
    private record SuccessResponse(String message, GestionCarburantGEDto data) {}
}