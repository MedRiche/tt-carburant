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
 *
 * MISE À JOUR :
 *   - Si l'utilisateur est un CONDUCTEUR (specialite = "Conducteur"),
 *     on délègue automatiquement au ConducteurVehiculeController
 *     via la logique de matching nom/email.
 *   - Si c'est un TECHNICIEN normal, il voit tous les véhicules de ses zones.
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
     * - Conducteur : liste des véhicules à son nom
     * - Technicien : tous les véhicules de ses zones
     */
    @GetMapping
    public ResponseEntity<?> getMesVehicules() {
        try {
            Utilisateur utilisateur = getConnecte();

            if (isConducteur(utilisateur)) {
                // Conducteur → véhicules assignés à son nom
                List<VehiculeDto> vehicules = findVehiculesParConducteur(utilisateur);
                return ResponseEntity.ok(vehicules);
            } else {
                // Technicien → tous les véhicules de ses zones
                List<VehiculeDto> vehicules = getVehiculesDeZonesDuTechnicien(utilisateur);
                return ResponseEntity.ok(vehicules);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/technicien/vehicules/zone/{zoneId}
     * Retourne les véhicules d'une zone spécifique (technicien affecté uniquement).
     * Les conducteurs n'ont pas accès à cette route (403).
     */
    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<?> getVehiculesByZone(@PathVariable("zoneId") Long zoneId) {
        try {
            Utilisateur utilisateur = getConnecte();

            if (isConducteur(utilisateur)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Les conducteurs n'ont pas accès par zone."));
            }

            boolean estAffecte = affectationRepository.findByUtilisateur(utilisateur)
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
     * Détail d'un véhicule.
     * - Conducteur : seulement si c'est son véhicule
     * - Technicien : seulement si le véhicule est dans ses zones
     */
    @GetMapping("/{matricule:.+}")
    public ResponseEntity<?> getVehiculeDetail(@PathVariable("matricule") String matricule) {
        try {
            Utilisateur utilisateur = getConnecte();

            Vehicule vehicule = vehiculeRepository.findById(matricule)
                    .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));

            if (isConducteur(utilisateur)) {
                if (!isConducteurDuVehicule(utilisateur, vehicule)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new ErrorResponse("Vous n'êtes pas le conducteur de ce véhicule."));
                }
            } else {
                if (vehicule.getZone() == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new ErrorResponse("Ce véhicule n'est rattaché à aucune zone."));
                }
                Long zoneId = vehicule.getZone().getId();
                boolean estAffecte = affectationRepository.findByUtilisateur(utilisateur)
                        .stream()
                        .anyMatch(a -> a.getZone().getId().equals(zoneId));
                if (!estAffecte) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new ErrorResponse("Vous n'avez pas accès à ce véhicule."));
                }
            }

            return ResponseEntity.ok(toDto(vehicule));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/technicien/vehicules/stats
     * Statistiques adaptées au rôle.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Utilisateur utilisateur = getConnecte();
            List<VehiculeDto> vehicules;

            if (isConducteur(utilisateur)) {
                vehicules = findVehiculesParConducteur(utilisateur);
            } else {
                vehicules = getVehiculesDeZonesDuTechnicien(utilisateur);
            }

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalVehicules", vehicules.size());
            stats.put("estConducteur", isConducteur(utilisateur));

            Map<String, Long> parType = vehicules.stream()
                    .collect(Collectors.groupingBy(
                            v -> v.getTypeCarburant() != null ? v.getTypeCarburant().name() : "INCONNU",
                            Collectors.counting()));
            stats.put("parTypeCarburant", parType);

            Map<String, Long> parTypeVehicule = vehicules.stream()
                    .filter(v -> v.getTypeVehicule() != null)
                    .collect(Collectors.groupingBy(VehiculeDto::getTypeVehicule, Collectors.counting()));
            stats.put("parTypeVehicule", parTypeVehicule);

            long visitesDepassees = vehicules.stream()
                    .filter(v -> v.getVisiteTechnique() != null
                            && v.getVisiteTechnique().isBefore(LocalDate.now()))
                    .count();
            stats.put("visitesDepassees", visitesDepassees);

            double kmTotal = vehicules.stream()
                    .mapToDouble(VehiculeDto::getKilometrageTotal)
                    .sum();
            stats.put("kilometrageTotalCumul", Math.round(kmTotal * 10.0) / 10.0);

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

    private Utilisateur getConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Non authentifié");
        }
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    /** Vérifie si l'utilisateur est un conducteur */
    private boolean isConducteur(Utilisateur u) {
        return "Conducteur".equalsIgnoreCase(u.getSpecialite());
    }

    /** Technicien classique → véhicules de toutes ses zones */
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

    /**
     * Conducteur → véhicules dont il est le conducteur assigné.
     * Matching basé sur email (prenom.nom@...) vs nomConducteur/prenomConducteur.
     */
    private List<VehiculeDto> findVehiculesParConducteur(Utilisateur conducteur) {
        return vehiculeRepository.findAll()
                .stream()
                .filter(v -> isConducteurDuVehicule(conducteur, v))
                .map(this::toDto)
                .sorted(Comparator.comparing(VehiculeDto::getMatricule))
                .collect(Collectors.toList());
    }

    /**
     * Multi-stratégie de matching conducteur ↔ véhicule.
     */
    boolean isConducteurDuVehicule(Utilisateur conducteur, Vehicule vehicule) {
        String nomV    = normaliser(vehicule.getNomConducteur());
        String prenomV = normaliser(vehicule.getPrenomConducteur());

        if (nomV.isEmpty() && prenomV.isEmpty()) return false;

        String email = conducteur.getEmail() != null ? conducteur.getEmail().toLowerCase() : "";
        String[] parts = extractPrenomNomFromEmail(email);
        String prenomEmail = parts[0]; // ex: "taoufik"
        String nomEmail    = parts[1]; // ex: "jebri"

        // Matching 1 : email strict (prenom.nom exact)
        boolean matchStrict = !nomEmail.isEmpty() && !prenomEmail.isEmpty()
                && nomV.equals(nomEmail) && prenomV.equals(prenomEmail);

        // Matching 2 : email partiel (un des deux champs)
        boolean matchPartiel = (!nomEmail.isEmpty() && nomV.equals(nomEmail))
                || (!prenomEmail.isEmpty() && prenomV.equals(prenomEmail));

        // Matching 3 : nom complet du profil vs (prenom + nom) du véhicule
        String nomProfil = normaliser(conducteur.getNom() != null ? conducteur.getNom() : "");
        String fullV = (prenomV + " " + nomV).trim();
        boolean matchProfil = !nomProfil.isEmpty() && (
                fullV.equals(nomProfil) ||
                        nomProfil.contains(nomV) && nomProfil.contains(prenomV)
        );

        return matchStrict || matchPartiel || matchProfil;
    }

    private String[] extractPrenomNomFromEmail(String email) {
        String local = email.contains("@") ? email.split("@")[0] : email;
        local = local.replaceAll("\\d+$", ""); // remove trailing digits
        if (local.contains(".")) {
            String[] p = local.split("\\.", 2);
            return new String[]{ normaliser(p[0]), normaliser(p[1]) };
        }
        return new String[]{ normaliser(local), "" };
    }

    private String normaliser(String s) {
        if (s == null || s.isEmpty()) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
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