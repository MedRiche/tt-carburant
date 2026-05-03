package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.CarburantVehiculeDto;
import com.example.ttcarburant.dto.CarburantVehiculeRequest;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.repository.UtilisateurRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import com.example.ttcarburant.repository.CarburantVehiculeRepository;
import com.example.ttcarburant.model.entity.GestionCarburantVehicule;
import com.example.ttcarburant.services.CarburantVehiculeService;
import jakarta.validation.Valid;
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
 * Étape 4 — Technicien/Conducteur : Gestion du carburant de ses véhicules.
 *
 * Routes :
 *   GET  /api/technicien/carburant/mes-vehicules          → liste des véhicules du technicien
 *   GET  /api/technicien/carburant/historique/{matricule} → historique carburant d'un véhicule
 *   GET  /api/technicien/carburant/prefill/{matricule}    → pré-remplissage mois précédent
 *   POST /api/technicien/carburant/saisir                 → enregistrer un ravitaillement
 *   GET  /api/technicien/carburant/stats/{matricule}      → analytics par véhicule
 *   GET  /api/technicien/carburant/dashboard              → dashboard global technicien
 */
@RestController
@RequestMapping("/api/technicien/carburant")
@PreAuthorize("hasRole('TECHNICIEN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class TechnicienCarburantController {

    private final UtilisateurRepository utilisateurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final CarburantVehiculeRepository carburantRepository;
    private final CarburantVehiculeService carburantVehiculeService;

    private static final String[] MOIS_LABELS = {
            "", "Janvier","Février","Mars","Avril","Mai","Juin",
            "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
    };

    public TechnicienCarburantController(
            UtilisateurRepository utilisateurRepository,
            VehiculeRepository vehiculeRepository,
            CarburantVehiculeRepository carburantRepository,
            CarburantVehiculeService carburantVehiculeService) {
        this.utilisateurRepository = utilisateurRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.carburantRepository = carburantRepository;
        this.carburantVehiculeService = carburantVehiculeService;
    }

    // ── GET mes véhicules (avec données carburant du mois courant) ─────────────

    @GetMapping("/mes-vehicules")
    public ResponseEntity<?> getMesVehicules() {
        try {
            Utilisateur u = getConnecte();
            List<Vehicule> vehicules = findVehiculesParUtilisateur(u);

            int moisCourant = LocalDate.now().getMonthValue();
            int anneeCourante = LocalDate.now().getYear();

            List<Map<String, Object>> result = vehicules.stream().map(v -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("matricule", v.getMatricule());
                item.put("marqueModele", v.getMarqueModele());
                item.put("typeVehicule", v.getTypeVehicule());
                item.put("typeCarburant", v.getTypeCarburant());
                item.put("prixCarburant", v.getPrixCarburant());
                item.put("coutDuMois", v.getCoutDuMois());
                item.put("kilometrageTotal", v.getKilometrageTotal());
                item.put("nomConducteur", v.getNomConducteur());
                item.put("prenomConducteur", v.getPrenomConducteur());
                if (v.getZone() != null) {
                    item.put("zoneId", v.getZone().getId());
                    item.put("zoneNom", v.getZone().getNom());
                }

                // Saisie du mois courant si existante
                Optional<GestionCarburantVehicule> moisOpt =
                        carburantRepository.findByVehiculeAndAnneeAndMois(v, anneeCourante, moisCourant);
                item.put("saisieExistante", moisOpt.isPresent());
                moisOpt.ifPresent(g -> {
                    item.put("distanceMois", g.getDistanceParcourue());
                    item.put("consoMoisLitres", g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
                    item.put("budgetDepasse", g.isBudgetDepasse());
                    item.put("depassementMontant", g.getDepassementMontant());
                    item.put("gestionId", g.getId());
                });

                return item;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── GET historique carburant d'un véhicule ────────────────────────────────

    @GetMapping("/historique/{matricule:.+}")
    public ResponseEntity<?> getHistorique(
            @PathVariable("matricule") String matricule,
            @RequestParam(required = false) Integer annee) {
        try {
            Utilisateur u = getConnecte();
            Vehicule v = findVehiculeAutorise(u, matricule);

            List<GestionCarburantVehicule> data;
            if (annee != null) {
                data = carburantRepository.findByVehiculeAndAnneeOrderByMois(v, annee);
            } else {
                data = carburantRepository.findByVehiculeOrderByAnneeDescMoisDesc(v);
            }

            List<Map<String, Object>> result = data.stream().map(g -> toHistoriqueMap(g)).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── GET préfill mois précédent ────────────────────────────────────────────

    @GetMapping("/prefill/{matricule:.+}")
    public ResponseEntity<?> getPrefill(
            @PathVariable("matricule") String matricule,
            @RequestParam int annee,
            @RequestParam int mois) {
        try {
            Utilisateur u = getConnecte();
            findVehiculeAutorise(u, matricule); // vérification accès

            return ResponseEntity.ok(carburantVehiculeService.getPrefillFromPreviousMonth(matricule, annee, mois));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── POST saisir un ravitaillement ─────────────────────────────────────────

    @PostMapping("/saisir")
    public ResponseEntity<?> saisir(@Valid @RequestBody CarburantVehiculeRequest req) {
        try {
            Utilisateur u = getConnecte();
            findVehiculeAutorise(u, req.getVehiculeMatricule()); // vérif accès

            CarburantVehiculeDto dto = carburantVehiculeService.saisir(req);

            if (dto.isBudgetDepasse()) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                                "message", "Ravitaillement enregistré ⚠️ Budget dépassé de " + dto.getDepassementMontant() + " DT",
                                "data", dto,
                                "alert", true
                        ));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Ravitaillement enregistré avec succès", "data", dto));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── PUT modifier une saisie ───────────────────────────────────────────────

    @PutMapping("/modifier/{id}")
    public ResponseEntity<?> modifier(
            @PathVariable Long id,
            @Valid @RequestBody CarburantVehiculeRequest req) {
        try {
            Utilisateur u = getConnecte();
            findVehiculeAutorise(u, req.getVehiculeMatricule());

            CarburantVehiculeDto dto = carburantVehiculeService.modifier(id, req);
            return ResponseEntity.ok(Map.of("message", "Saisie modifiée", "data", dto));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── GET stats / analytics par véhicule ───────────────────────────────────

    @GetMapping("/stats/{matricule:.+}")
    public ResponseEntity<?> getStats(
            @PathVariable("matricule") String matricule,
            @RequestParam(defaultValue = "0") int annee) {
        try {
            Utilisateur u = getConnecte();
            Vehicule v = findVehiculeAutorise(u, matricule);

            int targetAnnee = annee > 0 ? annee : LocalDate.now().getYear();
            List<GestionCarburantVehicule> data =
                    carburantRepository.findByVehiculeAndAnneeOrderByMois(v, targetAnnee);

            Map<String, Object> stats = buildStats(v, data, targetAnnee);
            return ResponseEntity.ok(stats);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── GET dashboard global technicien ──────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Utilisateur u = getConnecte();
            List<Vehicule> vehicules = findVehiculesParUtilisateur(u);

            int annee = LocalDate.now().getYear();
            int mois = LocalDate.now().getMonthValue();

            Map<String, Object> dashboard = new LinkedHashMap<>();
            dashboard.put("totalVehicules", vehicules.size());

            double totalKm = 0, totalLitres = 0, totalDT = 0;
            int nbBudgetDepasses = 0;
            List<Map<String, Object>> vehiculesResume = new ArrayList<>();

            for (Vehicule v : vehicules) {
                List<GestionCarburantVehicule> anneeData =
                        carburantRepository.findByVehiculeAndAnneeOrderByMois(v, annee);

                double vKm = anneeData.stream().mapToDouble(GestionCarburantVehicule::getDistanceParcourue).sum();
                double vL  = anneeData.stream().mapToDouble(g ->
                        Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir())).sum();
                double vDT = vL * v.getPrixCarburant();
                boolean hasBudgetDepasse = anneeData.stream().anyMatch(GestionCarburantVehicule::isBudgetDepasse);

                totalKm += vKm; totalLitres += vL; totalDT += vDT;
                if (hasBudgetDepasse) nbBudgetDepasses++;

                // Données mois courant
                Optional<GestionCarburantVehicule> moisCourant =
                        carburantRepository.findByVehiculeAndAnneeAndMois(v, annee, mois);

                Map<String, Object> vMap = new LinkedHashMap<>();
                vMap.put("matricule", v.getMatricule());
                vMap.put("marqueModele", v.getMarqueModele());
                vMap.put("kmAnnee", round3(vKm));
                vMap.put("litresAnnee", round3(vL));
                vMap.put("coutAnnee", round3(vDT));
                vMap.put("budgetDepasse", hasBudgetDepasse);
                vMap.put("saisiesMois", anneeData.size());
                vMap.put("moisCourantSaisi", moisCourant.isPresent());
                moisCourant.ifPresent(g -> {
                    vMap.put("kmMois", g.getDistanceParcourue());
                    vMap.put("budgetMoisDepasse", g.isBudgetDepasse());
                });
                vehiculesResume.add(vMap);
            }

            dashboard.put("totalKmAnnee", round3(totalKm));
            dashboard.put("totalLitresAnnee", round3(totalLitres));
            dashboard.put("totalCoutAnnee", round3(totalDT));
            dashboard.put("nbBudgetsDepasses", nbBudgetDepasses);
            dashboard.put("annee", annee);
            dashboard.put("vehicules", vehiculesResume);

            // Evolution mensuelle globale
            List<Map<String, Object>> evolution = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                final int fm = m;
                double mKm = 0, mL = 0;
                for (Vehicule v : vehicules) {
                    Optional<GestionCarburantVehicule> gOpt =
                            carburantRepository.findByVehiculeAndAnneeAndMois(v, annee, fm);
                    if (gOpt.isPresent()) {
                        GestionCarburantVehicule g = gOpt.get();
                        mKm += g.getDistanceParcourue();
                        mL  += Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
                    }
                }
                Map<String, Object> mMap = new LinkedHashMap<>();
                mMap.put("mois", m);
                mMap.put("label", MOIS_LABELS[m].substring(0, 3));
                mMap.put("km", round3(mKm));
                mMap.put("litres", round3(mL));
                evolution.add(mMap);
            }
            dashboard.put("evolutionMensuelle", evolution);

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Utilisateur getConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private Vehicule findVehiculeAutorise(Utilisateur u, String matricule) {
        Vehicule v = vehiculeRepository.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));

        List<Vehicule> autorises = findVehiculesParUtilisateur(u);
        boolean autorise = autorises.stream().anyMatch(av -> av.getMatricule().equals(matricule));

        if (!autorise) {
            throw new SecurityException("Accès refusé : vous n'avez pas accès à ce véhicule.");
        }
        return v;
    }

    private List<Vehicule> findVehiculesParUtilisateur(Utilisateur u) {
        if ("Conducteur".equalsIgnoreCase(u.getSpecialite())) {
            return vehiculeRepository.findAll().stream()
                    .filter(v -> isConducteurDuVehicule(u, v))
                    .collect(Collectors.toList());
        }
        // Technicien classique : tous véhicules de ses zones
        // (simplifié - en prod utiliser AffectationRepository)
        return vehiculeRepository.findAll();
    }

    private boolean isConducteurDuVehicule(Utilisateur u, Vehicule v) {
        String nomV    = normaliser(v.getNomConducteur());
        String prenomV = normaliser(v.getPrenomConducteur());
        if (nomV.isEmpty() && prenomV.isEmpty()) return false;

        String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
        String local = email.contains("@") ? email.split("@")[0] : email;
        local = local.replaceAll("\\d+$", "");
        String prenomE = "", nomE = "";
        if (local.contains(".")) {
            String[] parts = local.split("\\.", 2);
            prenomE = normaliser(parts[0]);
            nomE    = normaliser(parts[1]);
        } else {
            prenomE = normaliser(local);
        }

        boolean matchStrict = !nomE.isEmpty() && !prenomE.isEmpty()
                && nomV.equals(nomE) && prenomV.equals(prenomE);
        boolean matchPartiel = (!nomE.isEmpty() && nomV.equals(nomE))
                || (!prenomE.isEmpty() && prenomV.equals(prenomE));

        return matchStrict || matchPartiel;
    }

    private String normaliser(String s) {
        if (s == null || s.isEmpty()) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private Map<String, Object> toHistoriqueMap(GestionCarburantVehicule g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("annee", g.getAnnee());
        m.put("mois", g.getMois());
        m.put("periodeLabel", MOIS_LABELS[g.getMois()] + " " + g.getAnnee());
        m.put("indexDemarrageMois", g.getIndexDemarrageMois());
        m.put("indexFinMois", g.getIndexFinMois());
        m.put("montantRestantMoisPrecedent", g.getMontantRestantMoisPrecedent());
        m.put("ravitaillementMoisPrecedent", g.getRavitaillementMoisPrecedent());
        m.put("ravitaillementMois", g.getRavitaillementMois());
        m.put("totalRavitaillementLitres", g.getTotalRavitaillementLitres());
        m.put("quantiteRestanteReservoir", g.getQuantiteRestanteReservoir());
        m.put("distanceParcourue", g.getDistanceParcourue());
        m.put("pourcentageConsommation", g.getPourcentageConsommation());
        m.put("carburantDemandeDinars", g.getCarburantDemandeDinars());
        m.put("montantRestantReservoirFin", g.getMontantRestantReservoirFin());
        m.put("prixCarburant", g.getVehicule().getPrixCarburant());
        m.put("coutDuMois", g.getVehicule().getCoutDuMois());
        m.put("budgetDepasse", g.isBudgetDepasse());
        m.put("depassementMontant", g.getDepassementMontant());
        m.put("dateCreation", g.getDateCreation());
        double consoL = Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
        double coutReel = consoL * g.getVehicule().getPrixCarburant();
        double tauxBudget = g.getVehicule().getCoutDuMois() > 0
                ? round2(coutReel / g.getVehicule().getCoutDuMois() * 100) : 0;
        m.put("tauxBudget", tauxBudget);
        m.put("consoLitres", round3(consoL));
        m.put("coutReel", round3(coutReel));
        return m;
    }

    private Map<String, Object> buildStats(Vehicule v, List<GestionCarburantVehicule> data, int annee) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("matricule", v.getMatricule());
        stats.put("marqueModele", v.getMarqueModele());
        stats.put("annee", annee);
        stats.put("nbMoisSaisis", data.size());

        double totalKm   = data.stream().mapToDouble(GestionCarburantVehicule::getDistanceParcourue).sum();
        double totalL    = data.stream().mapToDouble(g -> Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir())).sum();
        double totalCout = totalL * v.getPrixCarburant();
        double budget    = v.getCoutDuMois() * data.size();

        stats.put("totalKm", round3(totalKm));
        stats.put("totalLitres", round3(totalL));
        stats.put("totalCout", round3(totalCout));
        stats.put("totalBudget", round3(budget));
        stats.put("tauxBudget", budget > 0 ? round2(totalCout / budget * 100) : 0);
        stats.put("rendementMoyen", totalKm > 0 ? round3(totalL / totalKm * 100) : 0);
        stats.put("nbBudgetsDepasses", data.stream().filter(GestionCarburantVehicule::isBudgetDepasse).count());

        // Evolution mensuelle
        List<Map<String, Object>> evolution = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            final int fm = m;
            Map<String, Object> mMap = new LinkedHashMap<>();
            mMap.put("mois", m);
            mMap.put("label", MOIS_LABELS[m].substring(0, 3));
            Optional<GestionCarburantVehicule> gOpt = data.stream().filter(g -> g.getMois() == fm).findFirst();
            if (gOpt.isPresent()) {
                GestionCarburantVehicule g = gOpt.get();
                double consoL = Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
                mMap.put("km", g.getDistanceParcourue());
                mMap.put("litres", round3(consoL));
                mMap.put("cout", round3(consoL * v.getPrixCarburant()));
                mMap.put("rendement", g.getDistanceParcourue() > 0 ? round3(consoL / g.getDistanceParcourue() * 100) : 0);
                mMap.put("budgetDepasse", g.isBudgetDepasse());
                mMap.put("budget", v.getCoutDuMois());
            } else {
                mMap.put("km", 0); mMap.put("litres", 0); mMap.put("cout", 0);
                mMap.put("rendement", 0); mMap.put("budgetDepasse", false);
                mMap.put("budget", v.getCoutDuMois());
            }
            evolution.add(mMap);
        }
        stats.put("evolution", evolution);
        return stats;
    }

    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private record ErrorResponse(String message) {}
}