package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.VehiculeDto;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.repository.UtilisateurRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
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
 * Étape 3 — Conducteur : consulter les véhicules assignés à son nom.
 *
 * Logique de matching :
 *   L'email du conducteur est au format prenom.nom@tunisietelecom.tn
 *   On extrait prénom + nom depuis l'email et on compare aux champs
 *   nomConducteur / prenomConducteur du véhicule (insensible à la casse,
 *   accents normalisés).
 *
 * Exemple : taoufik.jebri@tunisietelecom.tn
 *   → prénom extrait = "taoufik"
 *   → nom extrait   = "jebri"
 *   → véhicule matché si nomConducteur ≈ "JEBRI" ET prenomConducteur ≈ "TAOUFIK"
 */
@RestController
@RequestMapping("/api/conducteur/vehicules")
@PreAuthorize("hasRole('TECHNICIEN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class ConducteurVehiculeController {

    private final UtilisateurRepository utilisateurRepository;
    private final VehiculeRepository vehiculeRepository;

    public ConducteurVehiculeController(
            UtilisateurRepository utilisateurRepository,
            VehiculeRepository vehiculeRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.vehiculeRepository    = vehiculeRepository;
    }

    /**
     * GET /api/conducteur/vehicules
     * Retourne uniquement les véhicules dont le conducteur correspond
     * à l'utilisateur connecté (matching nom/prénom depuis l'email).
     */
    @GetMapping
    public ResponseEntity<?> getMesVehicules() {
        try {
            Utilisateur conducteur = getConnecte();
            List<VehiculeDto> vehicules = findVehiculesParConducteur(conducteur);
            return ResponseEntity.ok(vehicules);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/conducteur/vehicules/{matricule}
     * Détail d'un véhicule (seulement si le conducteur en est responsable).
     */
    @GetMapping("/{matricule:.+}")
    public ResponseEntity<?> getVehiculeDetail(
            @PathVariable("matricule") String matricule) {
        try {
            Utilisateur conducteur = getConnecte();

            Vehicule vehicule = vehiculeRepository.findById(matricule)
                    .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));

            if (!isConducteurDuVehicule(conducteur, vehicule)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Vous n'êtes pas le conducteur de ce véhicule."));
            }

            return ResponseEntity.ok(toDto(vehicule));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/conducteur/vehicules/stats
     * Statistiques des véhicules du conducteur.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Utilisateur conducteur = getConnecte();
            List<VehiculeDto> vehicules = findVehiculesParConducteur(conducteur);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalVehicules", vehicules.size());

            // Visites techniques dépassées
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

            // Par type de carburant
            Map<String, Long> parType = vehicules.stream()
                    .collect(Collectors.groupingBy(
                            v -> v.getTypeCarburant() != null ? v.getTypeCarburant().name() : "INCONNU",
                            Collectors.counting()));
            stats.put("parTypeCarburant", parType);

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

    /**
     * Trouve tous les véhicules dont le conducteur matche l'utilisateur.
     *
     * Stratégies de matching (par ordre de priorité) :
     *  1. Comparaison directe nom complet (prénom + " " + nom) depuis l'email
     *  2. Comparaison séparée prénom / nom
     *  3. Recherche par nom complet stocké dans le champ "nom" de l'utilisateur
     */
    private List<VehiculeDto> findVehiculesParConducteur(Utilisateur conducteur) {
        String email = conducteur.getEmail().toLowerCase();

        // Extraire prénom et nom depuis l'email : prenom.nom@domaine.tn
        String[] prenomNom = extractPrenomNomFromEmail(email);
        String prenomEmail = prenomNom[0]; // ex: "taoufik"
        String nomEmail    = prenomNom[1]; // ex: "jebri"

        // Nom complet depuis le profil utilisateur
        String nomComplet = normaliser(conducteur.getNom() != null ? conducteur.getNom() : "");

        List<Vehicule> tous = vehiculeRepository.findAll();

        return tous.stream()
                .filter(v -> isConducteurDuVehicule(conducteur, v))
                .map(this::toDto)
                .sorted(Comparator.comparing(VehiculeDto::getMatricule))
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si l'utilisateur connecté est bien le conducteur du véhicule.
     * Matching multi-stratégies (insensible à la casse et aux accents).
     */
    private boolean isConducteurDuVehicule(Utilisateur conducteur, Vehicule vehicule) {
        String nomV    = normaliser(vehicule.getNomConducteur());
        String prenomV = normaliser(vehicule.getPrenomConducteur());

        if (nomV.isEmpty() && prenomV.isEmpty()) return false;

        String email = conducteur.getEmail().toLowerCase();
        String[] parts = extractPrenomNomFromEmail(email);
        String prenomEmail = parts[0];
        String nomEmail    = parts[1];

        // Stratégie 1 : matching email ↔ champs véhicule
        boolean matchEmail =
                (!nomEmail.isEmpty()    && nomV.contains(nomEmail))    ||
                        (!prenomEmail.isEmpty() && prenomV.contains(prenomEmail));

        // Stratégie 2 : matching nom complet utilisateur ↔ champs véhicule
        String nomComplet = normaliser(conducteur.getNom() != null ? conducteur.getNom() : "");
        boolean matchNomComplet = false;
        if (!nomComplet.isEmpty()) {
            String fullV = (prenomV + " " + nomV).trim();
            // "taoufik jebri" contains "taoufik" and "jebri"
            matchNomComplet = nomComplet.contains(nomV) ||
                    nomComplet.contains(prenomV) ||
                    fullV.contains(normaliser(conducteur.getNom()));
        }

        // Stratégie 3 : matching strict prénom + nom
        boolean matchStrict = !nomEmail.isEmpty() && !prenomEmail.isEmpty()
                && nomV.equals(nomEmail) && prenomV.equals(prenomEmail);

        return matchStrict || matchEmail || matchNomComplet;
    }

    /**
     * Extrait prénom et nom depuis un email du type : prenom.nom@tunisietelecom.tn
     * Gère aussi les formats : prenom.nom2@... ou prenom@... etc.
     */
    private String[] extractPrenomNomFromEmail(String email) {
        String local = email.contains("@") ? email.split("@")[0] : email;
        // Supprimer un éventuel chiffre suffixe (ex: taoufik.jebri1 → taoufik.jebri)
        local = local.replaceAll("\\d+$", "");

        if (local.contains(".")) {
            String[] parts = local.split("\\.", 2);
            return new String[]{ normaliser(parts[0]), normaliser(parts[1]) };
        }
        // Pas de point → tout est le prénom, nom vide
        return new String[]{ normaliser(local), "" };
    }

    /** Supprime les accents et met en minuscules */
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