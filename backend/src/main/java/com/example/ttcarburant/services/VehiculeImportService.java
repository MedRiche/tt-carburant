package com.example.ttcarburant.services;

import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.model.enums.TypeCarburant;
import com.example.ttcarburant.repository.VehiculeRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class VehiculeImportService {

    private final VehiculeRepository vehiculeRepo;
    private final ZoneRepository zoneRepo;

    public VehiculeImportService(VehiculeRepository vehiculeRepo, ZoneRepository zoneRepo) {
        this.vehiculeRepo = vehiculeRepo;
        this.zoneRepo = zoneRepo;
    }

    /**
     * Importe les véhicules depuis un fichier Excel (format DAF 2026).
     * Structure : row 1 = en-têtes, row 2+ = données
     * Colonnes clés : 1=Matricule, 2=date_mise_service, 3=marque_modele, 4=type_vehicule,
     *                 5=Subdivision, 6=Central/CSC/ROC, 7=PRENOM, 8=NOM, 9=RSIDENCE,
     *                 10=TYPE_CARBURANT, 11=Prix, 12=coût_mois
     *
     * @param file      le fichier XLSX uploadé
     * @param zoneNom   le nom de la zone à affecter (doit exister en BDD)
     */
    @Transactional
    public Map<String, Object> importerVehicules(MultipartFile file, String zoneNom) throws Exception {

        int imported = 0;
        int updated  = 0;
        int skipped  = 0;
        List<String> errors = new ArrayList<>();

        // Résoudre la zone
        Zone zone = null;
        if (zoneNom != null && !zoneNom.isBlank()) {
            zone = zoneRepo.findByNom(zoneNom.trim())
                    .orElse(null);
            if (zone == null) {
                // Cherche une correspondance partielle insensible à la casse
                List<Zone> allZones = zoneRepo.findAll();
                for (Zone z : allZones) {
                    if (z.getNom().equalsIgnoreCase(zoneNom.trim())) {
                        zone = z;
                        break;
                    }
                }
            }
            if (zone == null) {
                errors.add("Zone introuvable : " + zoneNom + ". Le véhicule sera importé sans zone.");
            }
        }

        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            // On ne lit que la première feuille (premier mois = référence des véhicules)
            Sheet sheet = wb.getSheetAt(0);

            for (Row row : sheet) {
                int rowNum = row.getRowNum();
                // row 0 = ligne titre vide, row 1 = en-têtes → on commence à row 2
                if (rowNum < 2) continue;

                Cell c1 = row.getCell(1);
                if (c1 == null) continue;
                String matricule = getCellString(c1);
                if (matricule == null || matricule.isBlank()) continue;

                // Nettoyer le matricule : supprimer espaces, convertir en entier si numérique
                matricule = matricule.trim().replace(" ", "");
                // Les matricules peuvent être stockés comme nombre (ex: 356814.0) → nettoyer
                if (matricule.endsWith(".0")) {
                    matricule = matricule.substring(0, matricule.length() - 2);
                }

                try {
                    boolean exists = vehiculeRepo.existsByMatricule(matricule);
                    Vehicule v = exists
                            ? vehiculeRepo.findById(matricule).get()
                            : new Vehicule();

                    v.setMatricule(matricule);

                    // Col 2 : date mise en service
                    LocalDate dateMise = getCellDate(row.getCell(2));
                    if (dateMise != null) v.setDateMiseService(dateMise);
                    else if (!exists) v.setDateMiseService(LocalDate.now());

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

                    // Col 7 : prénom conducteur
                    String prenom = getCellString(row.getCell(7));
                    if (prenom != null) v.setPrenomConducteur(prenom.trim());

                    // Col 8 : nom conducteur
                    String nom = getCellString(row.getCell(8));
                    if (nom != null) v.setNomConducteur(nom.trim());

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

                    // Zone
                    if (zone != null) v.setZone(zone);

                    // Valeurs techniques optionnelles si présentes (colonnes supplémentaires DAF)
                    // Col 23 : visite technique (présente dans certains fichiers)
                    if (row.getLastCellNum() > 23) {
                        LocalDate visite = getCellDate(row.getCell(23));
                        if (visite != null) v.setVisiteTechnique(visite);
                    }

                    vehiculeRepo.save(v);

                    if (exists) updated++;
                    else imported++;

                } catch (Exception e) {
                    errors.add("Ligne " + (rowNum + 1) + " (matricule=" + matricule + ") : " + e.getMessage());
                    skipped++;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("updated",  updated);
        result.put("skipped",  skipped);
        result.put("total",    imported + updated + skipped);
        result.put("zone",     zone != null ? zone.getNom() : "Aucune");
        result.put("errors",   errors);
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getCellString(Cell c) {
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> {
                double d = c.getNumericCellValue();
                // Si c'est un entier, pas de décimale
                if (d == Math.floor(d) && !Double.isInfinite(d)) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case FORMULA -> {
                try { yield c.getStringCellValue().trim(); }
                catch (Exception e) {
                    try {
                        double d = c.getNumericCellValue();
                        if (d == Math.floor(d)) yield String.valueOf((long) d);
                        yield String.valueOf(d);
                    } catch (Exception e2) { yield null; }
                }
            }
            default -> null;
        };
    }

    private double getCellDouble(Cell c, double def) {
        if (c == null) return def;
        return switch (c.getCellType()) {
            case NUMERIC -> c.getNumericCellValue();
            case STRING  -> {
                try { yield Double.parseDouble(c.getStringCellValue().trim().replace(",", ".")); }
                catch (NumberFormatException e) { yield def; }
            }
            default -> def;
        };
    }

    private LocalDate getCellDate(Cell c) {
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            try {
                Date d = c.getDateCellValue();
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception e) { return null; }
        }
        if (c.getCellType() == CellType.STRING) {
            String s = c.getStringCellValue().trim();
            if (s.isBlank()) return null;
            // Essayer plusieurs formats
            String[] formats = { "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy" };
            for (String fmt : formats) {
                try {
                    return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern(fmt));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Normalise la valeur texte du type de carburant vers l'enum TypeCarburant.
     */
    private TypeCarburant parseTypeCarburant(String raw) {
        if (raw == null) return TypeCarburant.GASOIL_ORDINAIRE;
        String s = raw.trim().toUpperCase()
                .replace("É", "E").replace("È", "E").replace("À", "A");
        if (s.contains("ESSENCE") || s.contains("SUPER SAN") || s.contains("SP"))
            return TypeCarburant.ESSENCE;
        if (s.contains("50"))       return TypeCarburant.GASOIL_50;
        if (s.contains("SOUFRE") || s.contains("SOUFFRE") || s.contains("SS"))
            return TypeCarburant.GASOIL_SANS_SOUFRE;
        if (s.contains("GASOIL") || s.contains("DIESEL"))
            return TypeCarburant.GASOIL_ORDINAIRE;
        return TypeCarburant.GASOIL_ORDINAIRE;
    }
}