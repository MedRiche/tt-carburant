package com.example.ttcarburant.services;

import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.model.enums.TypeCarburant;
import com.example.ttcarburant.repository.VehiculeRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Importe les véhicules depuis un fichier Excel (format DAF 2026).
 *
 * CORRECTIFS :
 * 1. L'extraction des conducteurs se fait AVANT le try/catch véhicule.
 * 2. La création des comptes conducteurs se fait dans une transaction SÉPARÉE
 *    (REQUIRES_NEW) pour éviter le rollback global.
 * 3. Meilleure gestion du getCellString() pour les cellules de type FORMULA/NUMERIC.
 */
@Service
public class VehiculeImportService {

    private final VehiculeRepository            vehiculeRepo;
    private final ZoneRepository                zoneRepo;
    private final ConducteurUserCreationService conducteurService;

    public VehiculeImportService(VehiculeRepository vehiculeRepo,
                                 ZoneRepository zoneRepo,
                                 ConducteurUserCreationService conducteurService) {
        this.vehiculeRepo      = vehiculeRepo;
        this.zoneRepo          = zoneRepo;
        this.conducteurService = conducteurService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Point d'entrée principal — transaction globale pour les véhicules
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> importerVehicules(MultipartFile file, String zoneNom) throws Exception {

        int imported = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        // ── 1. Collecter les conducteurs AVANT tout traitement ──────────────
        //    → même si une ligne véhicule échoue, on a les noms
        List<Map<String, String>> conducteurs = new ArrayList<>();

        // ── 2. Résoudre la zone ─────────────────────────────────────────────
        Zone zone = null;
        if (zoneNom != null && !zoneNom.isBlank()) {
            zone = zoneRepo.findByNom(zoneNom.trim()).orElse(null);
            if (zone == null) {
                // Recherche insensible à la casse
                for (Zone z : zoneRepo.findAll()) {
                    if (z.getNom().equalsIgnoreCase(zoneNom.trim())) {
                        zone = z;
                        break;
                    }
                }
            }
            if (zone == null) {
                errors.add("Zone introuvable : « " + zoneNom + " »."
                        + " Les véhicules seront importés sans zone.");
            }
        }

        // ── 3. Lecture Excel ─────────────────────────────────────────────────
        try (InputStream is = file.getInputStream();
             Workbook wb   = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);

            for (Row row : sheet) {
                int rowNum = row.getRowNum();
                if (rowNum < 2) continue;   // ligne 0 = vide, ligne 1 = en-têtes

                Cell c1 = row.getCell(1);
                if (c1 == null) continue;
                String matricule = getCellString(c1);
                if (matricule == null || matricule.isBlank()) continue;

                matricule = matricule.trim().replace(" ", "");
                if (matricule.endsWith(".0"))
                    matricule = matricule.substring(0, matricule.length() - 2);

                // ── EXTRACTION DU CONDUCTEUR — HORS try/catch véhicule ──────
                String prenom = getCellString(row.getCell(7));
                String nom    = getCellString(row.getCell(8));

                String prenomTrimmed = (prenom != null) ? prenom.trim() : "";
                String nomTrimmed    = (nom    != null) ? nom.trim()    : "";

                // Éviter les doublons dans la liste de collecte
                if (!prenomTrimmed.isEmpty() || !nomTrimmed.isEmpty()) {
                    Map<String, String> cond = new HashMap<>();
                    cond.put("prenom", prenomTrimmed);
                    cond.put("nom",    nomTrimmed);
                    conducteurs.add(cond);
                }

                // ── Traitement du véhicule ───────────────────────────────────
                try {
                    boolean exists = vehiculeRepo.existsByMatricule(matricule);
                    Vehicule v = exists
                            ? vehiculeRepo.findById(matricule).orElse(new Vehicule())
                            : new Vehicule();

                    v.setMatricule(matricule);

                    // Col 2 : date mise en service
                    LocalDate dateMise = getCellDate(row.getCell(2));
                    if (dateMise != null)  v.setDateMiseService(dateMise);
                    else if (!exists)      v.setDateMiseService(LocalDate.now());

                    // Col 3 : marque/modèle
                    String marque = getCellString(row.getCell(3));
                    v.setMarqueModele(marque != null ? marque.trim() : "Inconnu");

                    // Col 4 : type véhicule
                    String typeV = getCellString(row.getCell(4));
                    v.setTypeVehicule(typeV != null ? typeV.trim() : "Véhicule");

                    // Col 5 : subdivision
                    String subdiv = getCellString(row.getCell(5));
                    if (subdiv != null) v.setSubdivision(subdiv.trim());

                    // Col 6 : centre/CSC/ROC
                    String centre = getCellString(row.getCell(6));
                    if (centre != null) v.setCentre(centre.trim());

                    // Col 7 & 8 : conducteur (déjà extraits plus haut)
                    if (!prenomTrimmed.isEmpty()) v.setPrenomConducteur(prenomTrimmed);
                    if (!nomTrimmed.isEmpty())    v.setNomConducteur(nomTrimmed);

                    // Col 9 : résidence service
                    String residence = getCellString(row.getCell(9));
                    if (residence != null) v.setResidenceService(residence.trim());

                    // Col 10 : type carburant
                    String typeCarb = getCellString(row.getCell(10));
                    v.setTypeCarburant(parseTypeCarburant(typeCarb));

                    // Col 11 : prix carburant
                    double prix = getCellDouble(row.getCell(11), 2.0);
                    v.setPrixCarburant(prix);

                    // Col 12 : coût du mois
                    double cout = getCellDouble(row.getCell(12), 200.0);
                    v.setCoutDuMois(cout);

                    if (zone != null) v.setZone(zone);

                    // Col 23 : visite technique (si présente)
                    if (row.getLastCellNum() > 23) {
                        LocalDate visite = getCellDate(row.getCell(23));
                        if (visite != null) v.setVisiteTechnique(visite);
                    }

                    vehiculeRepo.save(v);
                    if (exists) updated++; else imported++;

                } catch (Exception e) {
                    errors.add("Ligne " + (rowNum + 1) + " (matricule=" + matricule + ") : "
                            + e.getMessage());
                    skipped++;
                }
            }
        }

        // ── 4. Créer les comptes conducteurs dans une transaction SÉPARÉE ───
        //    Utilise REQUIRES_NEW → ne rollback pas la transaction des véhicules
        List<ConducteurUserCreationService.ConducteurCreationResult> conducteurResults =
                conducteurService.creerComptesConducteurs(conducteurs);

        long nbCreated   = conducteurResults.stream()
                .filter(r -> "CREATED".equals(r.statut)).count();
        long nbExistants = conducteurResults.stream()
                .filter(r -> "ALREADY_EXISTS".equals(r.statut)).count();

        List<Map<String, String>> conducteursDetails = new ArrayList<>();
        for (var r : conducteurResults) {
            if ("CREATED".equals(r.statut)) {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("nomComplet", r.nomComplet);
                d.put("email",      r.email);
                d.put("statut",     r.statut);
                conducteursDetails.add(d);
            }
        }

        // ── 5. Résultat final ────────────────────────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported",             imported);
        result.put("updated",              updated);
        result.put("skipped",              skipped);
        result.put("total",                imported + updated + skipped);
        result.put("zone",                 zone != null ? zone.getNom() : "Aucune");
        result.put("errors",               errors);
        result.put("conducteursCreated",   nbCreated);
        result.put("conducteursExistants", nbExistants);
        result.put("conducteursDetails",   conducteursDetails);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lit une cellule et retourne sa valeur sous forme de String.
     * Gère correctement les cellules numériques (entières vs décimales),
     * les formules évaluées et les booléens.
     */
    private String getCellString(Cell c) {
        if (c == null) return null;
        switch (c.getCellType()) {
            case STRING:
                String s = c.getStringCellValue();
                return (s == null || s.isBlank()) ? null : s.trim();

            case NUMERIC:
                double d = c.getNumericCellValue();
                // Éviter "354440.0" → "354440"
                if (d == Math.floor(d) && !Double.isInfinite(d))
                    return String.valueOf((long) d);
                return String.valueOf(d);

            case BOOLEAN:
                return String.valueOf(c.getBooleanCellValue());

            case FORMULA:
                // Essayer d'abord la valeur string en cache
                try {
                    String sv = c.getStringCellValue();
                    if (sv != null && !sv.isBlank()) return sv.trim();
                } catch (Exception ignored) {}
                // Puis la valeur numérique
                try {
                    double dv = c.getNumericCellValue();
                    if (dv == Math.floor(dv) && !Double.isInfinite(dv))
                        return String.valueOf((long) dv);
                    return String.valueOf(dv);
                } catch (Exception ignored) {}
                return null;

            case BLANK:
            case _NONE:
            default:
                return null;
        }
    }

    private double getCellDouble(Cell c, double defaultVal) {
        if (c == null) return defaultVal;
        switch (c.getCellType()) {
            case NUMERIC:
                return c.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(c.getStringCellValue().trim().replace(",", "."));
                } catch (NumberFormatException e) {
                    return defaultVal;
                }
            case FORMULA:
                try { return c.getNumericCellValue(); }
                catch (Exception e) { return defaultVal; }
            default:
                return defaultVal;
        }
    }

    private LocalDate getCellDate(Cell c) {
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            try {
                return c.getDateCellValue()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            } catch (Exception e) {
                return null;
            }
        }
        if (c.getCellType() == CellType.STRING) {
            String s = c.getStringCellValue().trim();
            if (s.isBlank()) return null;
            String[] formats = {"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy"};
            for (String fmt : formats) {
                try {
                    return LocalDate.parse(s,
                            java.time.format.DateTimeFormatter.ofPattern(fmt));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private TypeCarburant parseTypeCarburant(String raw) {
        if (raw == null) return TypeCarburant.GASOIL_ORDINAIRE;
        String s = raw.trim().toUpperCase()
                .replace("É", "E").replace("È", "E")
                .replace("À", "A").replace("'", "");

        if (s.contains("ESSENCE") || s.contains("SUPER SAN") || s.contains("SP95")
                || s.contains("SP98"))
            return TypeCarburant.ESSENCE;
        if (s.contains("SANS SOUFRE") || s.contains("SS"))
            return TypeCarburant.GASOIL_SANS_SOUFRE;
        if (s.contains("50"))
            return TypeCarburant.GASOIL_50;
        if (s.contains("GASOIL") || s.contains("DIESEL") || s.contains("GO"))
            return TypeCarburant.GASOIL_ORDINAIRE;

        return TypeCarburant.GASOIL_ORDINAIRE;
    }
}