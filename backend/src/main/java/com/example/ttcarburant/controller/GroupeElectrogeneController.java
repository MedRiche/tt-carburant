package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.GroupeElectrogene.GroupeElectrogeneDto;
import com.example.ttcarburant.services.GroupeElectrogeneService;
import com.example.ttcarburant.services.GroupeElectrogeneImportService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controller REST — Groupes Électrogènes
 * URL de base : /api/admin/groupes-electrogenes
 *
 * FIX appliqués :
 *  1. Route /zone/{zoneId} déclarée AVANT /{site} pour éviter l'ambiguïté Spring MVC.
 *  2. @CrossOrigin explicite.
 *  3. Toutes les exceptions catchées → 400/500 JSON propre (plus de "No static resource").
 */
@RestController
@RequestMapping("/api/admin/groupes-electrogenes")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class GroupeElectrogeneController {

    private static final Logger log = LoggerFactory.getLogger(GroupeElectrogeneController.class);

    private final GroupeElectrogeneService       service;
    private final GroupeElectrogeneImportService importService;

    public GroupeElectrogeneController(GroupeElectrogeneService service,
                                       GroupeElectrogeneImportService importService) {
        this.service       = service;
        this.importService = importService;
    }

    // ── GET ALL ────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            List<GroupeElectrogeneDto> list = service.getAll();
            log.info("GET /groupes-electrogenes → {} résultats", list.size());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("Erreur GET /groupes-electrogenes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur serveur : " + e.getMessage()));
        }
    }

    // ── GET BY ZONE  (doit être avant /{site} !) ──────────────────────────

    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<?> getByZone(@PathVariable Long zoneId) {
        try {
            return ResponseEntity.ok(service.getByZone(zoneId));
        } catch (Exception e) {
            log.error("Erreur GET /groupes-electrogenes/zone/{}", zoneId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── GET BY SITE ────────────────────────────────────────────────────────

    @GetMapping("/{site}")
    public ResponseEntity<?> getBySite(@PathVariable String site) {
        try {
            return ResponseEntity.ok(service.getBySite(site));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── CREATE ─────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> creer(@Valid @RequestBody GroupeElectrogeneDto req) {
        try {
            GroupeElectrogeneDto created = service.creer(req);
            log.info("Groupe créé : {}", created.getSite());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Erreur POST /groupes-electrogenes", e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────

    @PutMapping("/{site}")
    public ResponseEntity<?> modifier(@PathVariable String site,
                                      @Valid @RequestBody GroupeElectrogeneDto req) {
        try {
            return ResponseEntity.ok(service.modifier(site, req));
        } catch (Exception e) {
            log.error("Erreur PUT /groupes-electrogenes/{}", site, e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{site}")
    public ResponseEntity<?> supprimer(@PathVariable String site) {
        try {
            service.supprimer(site);
            return ResponseEntity.ok(new MessageResponse("Groupe électrogène supprimé"));
        } catch (Exception e) {
            log.error("Erreur DELETE /groupes-electrogenes/{}", site, e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── IMPORT EXCEL ───────────────────────────────────────────────────────

    @PostMapping("/import")
    public ResponseEntity<?> importerExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "zoneNom", required = false) String zoneNom) {
        try {
            Map<String, Object> result = importService.importerDepuisExcel(file, zoneNom);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erreur import Excel groupes-electrogenes", e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Inner records ──────────────────────────────────────────────────────

    private record ErrorResponse(String message) {}
    private record MessageResponse(String message) {}
}