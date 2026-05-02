package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.VehiculeDto;
import com.example.ttcarburant.model.entity.AffectationUtilisateurZone;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.repository.AffectationUtilisateurZoneRepository;
import com.example.ttcarburant.repository.UtilisateurRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import com.example.ttcarburant.model.entity.Vehicule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Étape 3 — Technicien : Consulter les véhicules des zones affectées.
 */
@RestController
@RequestMapping("/api/technicien/vehicules")
@PreAuthorize("hasRole('TECHNICIEN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class TechnicienVehiculeController {

    private final UtilisateurRepository utilisateurRepository;
    private final AffectationUtilisateurZoneRepository affectationRepository;
    private final VehiculeRepository vehiculeRepository;

    public TechnicienVehiculeController(
            UtilisateurRepository utilisateurRepository,
            AffectationUtilisateurZoneRepository affectationRepository,
            VehiculeRepository vehiculeRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.affectationRepository = affectationRepository;
        this.vehiculeRepository    = vehiculeRepository;
    }

    /**
     * GET /api/technicien/vehicules
     * Retourne tous les véhicules des zones affectées au technicien connecté.
     */
    @GetMapping
    public ResponseEntity<?> getMesVehicules() {
        try {
            Utilisateur technicien = getTechnicienConnecte();
            List<VehiculeDto> vehicules = getVehiculesDeZonesDuTechnicien(technicien);
            return ResponseEntity.ok(vehicules);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/technicien/vehicules/zone/{zoneId}
     * Retourne les véhicules d'une zone spécifique (si le technicien y est affecté).
     */
    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<?> getVehiculesByZone(@PathVariable("zoneId") Long zoneId) {
        try {
            Utilisateur technicien = getTechnicienConnecte();

            // Vérifier que le technicien est bien affecté à cette zone
            boolean estAffecte = affectationRepository.findByUtilisateur(technicien)
                    .stream()
                    .anyMatch(a -> a.getZone().getId().equals(zoneId));

            if (!estAffecte) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Vous n'êtes pas affecté à cette zone."));
            }

            List<VehiculeDto> vehicules = vehiculeRepository.findByZone_Id(zoneId)
                    .stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vehicules);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/technicien/vehicules/{matricule}
     * Retourne le détail d'un véhicule (si le technicien est affecté à sa zone).
     */
    @GetMapping("/{matricule:.+}")
    public ResponseEntity<?> getVehiculeDetail(@PathVariable("matricule") String matricule) {
        try {
            Utilisateur technicien = getTechnicienConnecte();

            Vehicule vehicule = vehiculeRepository.findById(matricule)
                    .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));

            if (vehicule.getZone() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Ce véhicule n'est rattaché à aucune zone."));
            }

            // Vérifier l'accès
            Long zoneId = vehicule.getZone().getId();
            boolean estAffecte = affectationRepository.findByUtilisateur(technicien)
                    .stream()
                    .anyMatch(a -> a.getZone().getId().equals(zoneId));

            if (!estAffecte) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Vous n'avez pas accès à ce véhicule."));
            }

            return ResponseEntity.ok(toDto(vehicule));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/technicien/vehicules/stats
     * Statistiques rapides sur les véhicules du technicien.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Utilisateur technicien = getTechnicienConnecte();
            List<VehiculeDto> vehicules = getVehiculesDeZonesDuTechnicien(technicien);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalVehicules", vehicules.size());

            // Par type carburant
            Map<String, Long> parType = vehicules.stream()
                    .collect(Collectors.groupingBy(
                            v -> v.getTypeCarburant() != null ? v.getTypeCarburant().name() : "INCONNU",
                            Collectors.counting()));
            stats.put("parTypeCarburant", parType);

            // Par type de véhicule
            Map<String, Long> parTypeVehicule = vehicules.stream()
                    .filter(v -> v.getTypeVehicule() != null)
                    .collect(Collectors.groupingBy(VehiculeDto::getTypeVehicule, Collectors.counting()));
            stats.put("parTypeVehicule", parTypeVehicule);

            // Visites techniques dépassées (date passée)
            long visitesDepassees = vehicules.stream()
                    .filter(v -> v.getVisiteTechnique() != null
                            && v.getVisiteTechnique().isBefore(LocalDate.now()))
                    .count();
            stats.put("visitesDepassees", visitesDepassees);

            // Kilométrage total
            double kmTotal = vehicules.stream()
                    .mapToDouble(VehiculeDto::getKilometrageTotal)
                    .sum();
            stats.put("kilometrageTotalCumul", Math.round(kmTotal * 10.0) / 10.0);

            // Coût total du mois
            double coutMois = vehicules.stream()
                    .mapToDouble(VehiculeDto::getCoutDuMois)
                    .sum();
            stats.put("coutTotalMois", Math.round(coutMois * 1000.0) / 1000.0);

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

    private List<VehiculeDto> getVehiculesDeZonesDuTechnicien(Utilisateur technicien) {
        List<AffectationUtilisateurZone> affectations =
                affectationRepository.findByUtilisateur(technicien);

        return affectations.stream()
                .flatMap(aff -> vehiculeRepository.findByZone_Id(aff.getZone().getId()).stream())
                .distinct()
                .map(this::toDto)
                .sorted(Comparator.comparing(VehiculeDto::getMatricule))
                .collect(Collectors.toList());
    }

    private VehiculeDto toDto(Vehicule v) {
        VehiculeDto dto = new VehiculeDto();
        dto.setMatricule(v.getMatricule());
        dto.setDateMiseService(v.getDateMiseService());
        dto.setMarqueModele(v.getMarqueModele());
        dto.setTypeVehicule(v.getTypeVehicule());
        dto.setSubdivision(v.getSubdivision());
        dto.setCentre(v.getCentre());
        dto.setResidenceService(v.getResidenceService());
        dto.setNomConducteur(v.getNomConducteur());
        dto.setPrenomConducteur(v.getPrenomConducteur());
        dto.setTypeCarburant(v.getTypeCarburant());
        dto.setPrixCarburant(v.getPrixCarburant());
        dto.setIndexVidange(v.getIndexVidange());
        dto.setVisiteTechnique(v.getVisiteTechnique());
        dto.setIndexPneumatique(v.getIndexPneumatique());
        dto.setKilometrageTotal(v.getKilometrageTotal());
        dto.setConsommationDinarsCumul(v.getConsommationDinarsCumul());
        dto.setConsommationLitresCumul(v.getConsommationLitresCumul());
        dto.setCoutDuMois(v.getCoutDuMois());
        dto.setCroxChaine(v.getCroxChaine());
        dto.setIndexBatterie(v.getIndexBatterie());
        if (v.getZone() != null) {
            dto.setZoneId(v.getZone().getId());
            dto.setZoneNom(v.getZone().getNom());
        }
        return dto;
    }

    private record ErrorResponse(String message) {}
}