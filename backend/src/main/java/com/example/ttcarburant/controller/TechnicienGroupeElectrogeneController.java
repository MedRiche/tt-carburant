package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.GroupeElectrogene.GroupeElectrogeneDto;
import com.example.ttcarburant.model.entity.AffectationUtilisateurZone;
import com.example.ttcarburant.model.entity.GroupeElectrogene;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.repository.AffectationUtilisateurZoneRepository;
import com.example.ttcarburant.repository.GroupeElectrogeneRepository;
import com.example.ttcarburant.repository.UtilisateurRepository;
import com.example.ttcarburant.services.GroupeElectrogeneService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Étape 3 — Technicien : Consulter les groupes électrogènes des zones affectées.
 */
@RestController
@RequestMapping("/api/technicien/groupes-electrogenes")
@PreAuthorize("hasRole('TECHNICIEN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class TechnicienGroupeElectrogeneController {

    private final UtilisateurRepository utilisateurRepository;
    private final AffectationUtilisateurZoneRepository affectationRepository;
    private final GroupeElectrogeneRepository geRepository;
    private final GroupeElectrogeneService geService;

    public TechnicienGroupeElectrogeneController(
            UtilisateurRepository utilisateurRepository,
            AffectationUtilisateurZoneRepository affectationRepository,
            GroupeElectrogeneRepository geRepository,
            GroupeElectrogeneService geService) {
        this.utilisateurRepository = utilisateurRepository;
        this.affectationRepository = affectationRepository;
        this.geRepository          = geRepository;
        this.geService             = geService;
    }

    /**
     * GET /api/technicien/groupes-electrogenes
     * Retourne tous les GE des zones affectées au technicien connecté.
     */
    @GetMapping
    public ResponseEntity<?> getMesGroupes() {
        try {
            Utilisateur technicien = getTechnicienConnecte();
            List<GroupeElectrogeneDto> groupes = getGroupesDuTechnicien(technicien);
            return ResponseEntity.ok(groupes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/technicien/groupes-electrogenes/zone/{zoneId}
     * Retourne les GE d'une zone spécifique (si le technicien y est affecté).
     */
    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<?> getGroupesByZone(@PathVariable("zoneId") Long zoneId) {
        try {
            Utilisateur technicien = getTechnicienConnecte();

            boolean estAffecte = affectationRepository.findByUtilisateur(technicien)
                    .stream()
                    .anyMatch(a -> a.getZone().getId().equals(zoneId));

            if (!estAffecte) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Vous n'êtes pas affecté à cette zone."));
            }

            List<GroupeElectrogeneDto> groupes = geRepository.findByZoneId(zoneId)
                    .stream()
                    .map(ge -> {
                        try { return geService.toDto(ge); }
                        catch (Exception ex) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(groupes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/technicien/groupes-electrogenes/{site}
     * Détail d'un GE (si le technicien a accès à sa zone).
     */
    @GetMapping("/{site:.+}")
    public ResponseEntity<?> getGroupeDetail(@PathVariable("site") String site) {
        try {
            Utilisateur technicien = getTechnicienConnecte();

            GroupeElectrogene ge = geRepository.findBySite(site)
                    .orElseThrow(() -> new RuntimeException("Groupe électrogène non trouvé : " + site));

            if (ge.getZone() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Ce GE n'est rattaché à aucune zone."));
            }

            Long zoneId = ge.getZone().getId();
            boolean estAffecte = affectationRepository.findByUtilisateur(technicien)
                    .stream()
                    .anyMatch(a -> a.getZone().getId().equals(zoneId));

            if (!estAffecte) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Vous n'avez pas accès à ce groupe électrogène."));
            }

            return ResponseEntity.ok(geService.toDto(ge));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/technicien/groupes-electrogenes/stats
     * Statistiques rapides sur les GE du technicien.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Utilisateur technicien = getTechnicienConnecte();
            List<GroupeElectrogeneDto> groupes = getGroupesDuTechnicien(technicien);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalGroupes", groupes.size());

            // Par type carburant
            Map<String, Long> parType = groupes.stream()
                    .collect(Collectors.groupingBy(
                            g -> g.getTypeCarburant() != null ? g.getTypeCarburant().name() : "INCONNU",
                            Collectors.counting()));
            stats.put("parTypeCarburant", parType);

            // Puissance totale
            double puissanceTotale = groupes.stream()
                    .filter(g -> g.getPuissanceKVA() != null)
                    .mapToDouble(GroupeElectrogeneDto::getPuissanceKVA)
                    .sum();
            stats.put("puissanceTotaleKVA", Math.round(puissanceTotale * 10.0) / 10.0);

            // Cartes expirées
            long cartesExpirees = groupes.stream()
                    .filter(g -> {
                        if (g.getDateExpiration() == null || g.getDateExpiration().isBlank()) return false;
                        try {
                            java.time.YearMonth exp = java.time.YearMonth.parse(
                                    g.getDateExpiration(), DateTimeFormatter.ofPattern("yyyy-MM"));
                            return exp.isBefore(java.time.YearMonth.now());
                        } catch (Exception ex) { return false; }
                    })
                    .count();
            stats.put("cartesExpirees", cartesExpirees);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Utilisateur getTechnicienConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Non authentifié");
        }
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Technicien non trouvé"));
    }

    private List<GroupeElectrogeneDto> getGroupesDuTechnicien(Utilisateur technicien) {
        List<AffectationUtilisateurZone> affectations =
                affectationRepository.findByUtilisateur(technicien);

        return affectations.stream()
                .flatMap(aff -> geRepository.findByZoneId(aff.getZone().getId()).stream())
                .distinct()
                .map(ge -> {
                    try { return geService.toDto(ge); }
                    catch (Exception ex) { return null; }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(GroupeElectrogeneDto::getSite))
                .collect(Collectors.toList());
    }

    private record ErrorResponse(String message) {}
}