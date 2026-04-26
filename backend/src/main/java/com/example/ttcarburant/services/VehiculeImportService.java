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

/**
 * Service d'import Excel des véhicules.
 *
 * AJOUT : après l'import, les conducteurs dont le compte n'existe pas encore
 * sont créés automatiquement avec StatutCompte = EN_ATTENTE.
 * L'admin les retrouvera dans "Gestion des Utilisateurs → En attente".
 */
@Service
public class VehiculeImportService {

    private final VehiculeRepository vehiculeRepo;
    private final ZoneRepository zoneRepo;
    private final ConducteurUserCreationService conducteurService;

    public VehiculeImportService(VehiculeRepository vehiculeRepo,
                                 ZoneRepository zoneRepo,
                                 ConducteurUserCreationService conducteurService) {
        this.vehiculeRepo      = vehiculeRepo;
        this.zoneRepo          = zoneRepo;
        this.conducteurService = conducteurService;
    }

    @Transactional
    public Map<String, Object> importerVehicules(MultipartFile file, String zoneNom) throws Exception {

        int imported = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        // Collecter les conducteurs pour création automatique de compte
        List<Map<String, String>> conducteurs = new ArrayList<>();

        // Résoudre la zone
        Zone zone = null;
        if (zoneNom != null && !zoneNom.isBlank()) {
            zone = zoneRepo.findByNom(zoneNom.trim()).orElse(null);
            if (zone == null) {
                for (Zone z : zoneRepo.findAll()) {
                    if (z.getNom().equalsIgnoreCase(zoneNom.trim())) { zone = z; break; }
                }
            }
            if (zone == null) errors.add("Zone introuvable : " + zoneNom + ". Véhicules importés sans zone.");
        }

        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);

            for (Row row : sheet) {
                int rowNum = row.getRowNum();
                if (rowNum < 2) continue;

                Cell c1 = row.getCell(1);
                if (c1 == null) continue;
                String matricule = getCellString(c1);
                if (matricule == null || matricule.isBlank()) continue;

                matricule = matricule.trim().replace(" ", "");
                if (matricule.endsWith(".0")) matricule = matricule.substring(0, matricule.length() - 2);

                try {
                    boolean exists = vehiculeRepo.existsByMatricule(matricule);
                    Vehicule v = exists ? vehiculeRepo.findById(matricule).get() : new Vehicule();

                    v.setMatricule(matricule);

                    LocalDate dateMise = getCellDate(row.getCell(2));
                    if (dateMise != null) v.setDateMiseService(dateMise);
                    else if (!exists) v.setDateMiseService(LocalDate.now());

                    String marque = getCellString(row.getCell(3));
                    v.setMarqueModele(marque != null ? marque.trim() : "Inconnu");

                    String typeV = getCellString(row.getCell(4));
                    v.setTypeVehicule(typeV != null ? typeV.trim() : "Véhicule");

                    String subdiv = getCellString(row.getCell(5));
                    if (subdiv != null) v.setSubdivision(subdiv.trim());

                    String centre = getCellString(row.getCell(6));
                    if (centre != null) v.setCentre(centre.trim());

                    // Col 7 : prénom conducteur
                    String prenom = getCellString(row.getCell(7));
                    if (prenom != null) v.setPrenomConducteur(prenom.trim());

                    // Col 8 : nom conducteur
                    String nom = getCellString(row.getCell(8));
                    if (nom != null) v.setNomConducteur(nom.trim());

                    // ── Collecter conducteur pour création de compte ──────────
                    if ((prenom != null && !prenom.isBlank()) || (nom != null && !nom.isBlank())) {
                        Map<String, String> c = new HashMap<>();
                        c.put("prenom", prenom != null ? prenom.trim() : "");
                        c.put("nom",    nom    != null ? nom.trim()    : "");
                        conducteurs.add(c);
                    }

                    String residence = getCellString(row.getCell(9));
                    if (residence != null) v.setResidenceService(residence.trim());

                    String typeCarb = getCellString(row.getCell(10));
                    v.setTypeCarburant(parseTypeCarburant(typeCarb));

                    double prix = getCellDouble(row.getCell(11), 2.0);
                    v.setPrixCarburant(prix);

                    double cout = getCellDouble(row.getCell(12), 200.0);
                    v.setCoutDuMois(cout);

                    if (zone != null) v.setZone(zone);

                    if (row.getLastCellNum() > 23) {
                        LocalDate visite = getCellDate(row.getCell(23));
                        if (visite != null) v.setVisiteTechnique(visite);
                    }

                    vehiculeRepo.save(v);
                    if (exists) updated++; else imported++;

                } catch (Exception e) {
                    errors.add("Ligne " + (rowNum + 1) + " (matricule=" + matricule + ") : " + e.getMessage());
                    skipped++;
                }
            }
        }

        // ── Créer les comptes conducteurs ─────────────────────────────────────
        List<ConducteurUserCreationService.ConducteurCreationResult> conducteurResults =
                conducteurService.creerComptesConducteurs(conducteurs);

        long nbCreated       = conducteurResults.stream().filter(r -> "CREATED".equals(r.statut)).count();
        long nbAlreadyExists = conducteurResults.stream().filter(r -> "ALREADY_EXISTS".equals(r.statut)).count();

        // Construire le détail des comptes créés pour retour à l'UI
        List<Map<String, String>> conducteurDetails = new ArrayList<>();
        for (var r : conducteurResults) {
            if ("CREATED".equals(r.statut)) {
                Map<String, String> d = new HashMap<>();
                d.put("nomComplet", r.nomComplet);
                d.put("email",      r.email);
                d.put("statut",     r.statut);
                conducteurDetails.add(d);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported",              imported);
        result.put("updated",               updated);
        result.put("skipped",               skipped);
        result.put("total",                 imported + updated + skipped);
        result.put("zone",                  zone != null ? zone.getNom() : "Aucune");
        result.put("errors",                errors);
        result.put("conducteursCreated",    nbCreated);
        result.put("conducteursExistants",  nbAlreadyExists);
        result.put("conducteursDetails",    conducteurDetails);
        return result;
    }

    // ── Helpers (identiques à la version originale) ───────────────────────────

    private String getCellString(Cell c) {
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> {
                double d = c.getNumericCellValue();
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
            String[] formats = { "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy" };
            for (String fmt : formats) {
                try { return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern(fmt)); }
                catch (Exception ignored) {}
            }
        }
        return null;
    }

    private TypeCarburant parseTypeCarburant(String raw) {
        if (raw == null) return TypeCarburant.GASOIL_ORDINAIRE;
        String s = raw.trim().toUpperCase()
                .replace("É", "E").replace("È", "E").replace("À", "A");
        if (s.contains("ESSENCE") || s.contains("SUPER SAN") || s.contains("SP")) return TypeCarburant.ESSENCE;
        if (s.contains("50"))       return TypeCarburant.GASOIL_50;
        if (s.contains("SOUFRE") || s.contains("SS"))   return TypeCarburant.GASOIL_SANS_SOUFRE;
        if (s.contains("GASOIL") || s.contains("DIESEL")) return TypeCarburant.GASOIL_ORDINAIRE;
        return TypeCarburant.GASOIL_ORDINAIRE;
    }
}