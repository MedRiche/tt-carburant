package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.GroupeElectrogene.GroupeElectrogeneDto;
import com.example.ttcarburant.services.GroupeElectrogeneService;
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
@RequestMapping("/api/admin/groupes-electrogenes")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class GroupeElectrogeneController {

    private final GroupeElectrogeneService service;
    private final GroupeElectrogeneImportService importService;

    public GroupeElectrogeneController(GroupeElectrogeneService service,
                                       GroupeElectrogeneImportService importService) {
        this.service = service;
        this.importService = importService;
    }

    @GetMapping
    public ResponseEntity<List<GroupeElectrogeneDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{site}")
    public ResponseEntity<?> getBySite(@PathVariable String site) {
        try {
            return ResponseEntity.ok(service.getBySite(site));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<List<GroupeElectrogeneDto>> getByZone(@PathVariable Long zoneId) {
        return ResponseEntity.ok(service.getByZone(zoneId));
    }

    @PostMapping
    public ResponseEntity<?> creer(@Valid @RequestBody GroupeElectrogeneDto req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(service.creer(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{site}")
    public ResponseEntity<?> modifier(@PathVariable String site,
                                      @Valid @RequestBody GroupeElectrogeneDto req) {
        try {
            return ResponseEntity.ok(service.modifier(site, req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{site}")
    public ResponseEntity<?> supprimer(@PathVariable String site) {
        try {
            service.supprimer(site);
            return ResponseEntity.ok(new MessageResponse("Groupe électrogène supprimé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

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
}