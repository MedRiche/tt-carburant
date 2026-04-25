package com.example.ttcarburant.services;

import com.example.ttcarburant.model.entity.GroupeElectrogene;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.model.enums.TypeCarburant;
import com.example.ttcarburant.repository.GroupeElectrogeneRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GroupeElectrogeneImportService {

    private static final Logger log = LoggerFactory.getLogger(GroupeElectrogeneImportService.class);

    private final GroupeElectrogeneRepository geRepo;
    private final ZoneRepository zoneRepo;

    public GroupeElectrogeneImportService(GroupeElectrogeneRepository geRepo, ZoneRepository zoneRepo) {
        this.geRepo   = geRepo;
        this.zoneRepo = zoneRepo;
    }

    /**
     * Détecte si la feuille est au format "DEUXIEME SEMESTRE"
     * en inspectant la cellule d'en-tête (ligne 1, index 2).
     */
    private boolean isDeuxiemeSemestreFormat(Sheet sheet) {
        Row header = sheet.getRow(1);
        if (header == null) return false;
        String cell2 = getStringCell(header.getCell(2));
        return cell2 != null && cell2.toLowerCase().contains("carte");
    }

    @Transactional
    public Map<String, Object> importerDepuisExcel(MultipartFile file, String zoneNom) throws Exception {
        int imported = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        Zone zone = null;
        if (zoneNom != null && !zoneNom.isBlank()) {
            zone = zoneRepo.findByNom(zoneNom).orElse(null);
            if (zone == null) errors.add("Zone non trouvée : " + zoneNom);
        }

        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
                log.info("Import feuille #{} : '{}'", si, sheet.getSheetName());
                boolean isS2 = isDeuxiemeSemestreFormat(sheet);

                for (Row row : sheet) {
                    if (row.getRowNum() <= 1) continue; // Ignorer les 2 lignes d'en-tête

                    String site;
                    String typeCarburantStr;
                    String puissanceStr;
                    Double tauxConso;
                    Double consoMax;
                    String typeCarte;
                    String numeroCarte;
                    String dateExpStr;
                    String codePIN;
                    String codePUK;
                    String utilisateurRoc;

                    if (isS2) {
                        site             = getStringCell(row.getCell(1));
                        typeCarte        = getStringCell(row.getCell(2));
                        numeroCarte      = getStringCell(row.getCell(3));
                        dateExpStr       = getStringCell(row.getCell(4));
                        codePIN          = getStringCellForPin(row.getCell(5));   // FIX: PIN numérique
                        codePUK          = getStringCellForPin(row.getCell(6));   // FIX: PUK numérique
                        utilisateurRoc   = getStringCell(row.getCell(7));
                        typeCarburantStr = getStringCell(row.getCell(8));
                        puissanceStr     = getStringCell(row.getCell(9));
                        tauxConso        = getDoubleCell(row.getCell(10));
                        consoMax         = getDoubleCell(row.getCell(11));
                    } else {
                        site             = getStringCell(row.getCell(1));
                        typeCarburantStr = getStringCell(row.getCell(2));
                        puissanceStr     = getStringCell(row.getCell(3));
                        tauxConso        = getDoubleCell(row.getCell(4));
                        consoMax         = getDoubleCell(row.getCell(5));
                        typeCarte        = getStringCell(row.getCell(16));
                        numeroCarte      = getStringCell(row.getCell(17));
                        dateExpStr       = getStringCell(row.getCell(18));
                        codePIN          = getStringCellForPin(row.getCell(19)); // FIX: PIN numérique
                        codePUK          = getStringCellForPin(row.getCell(20)); // FIX: PUK numérique
                        utilisateurRoc   = getStringCell(row.getCell(21));
                    }

                    if (site == null || site.isBlank()) continue;

                    // Ignorer les lignes "démonté"
                    Double puissance = null;
                    if (puissanceStr != null && !puissanceStr.equalsIgnoreCase("démonté")) {
                        try { puissance = Double.parseDouble(puissanceStr.trim()); }
                        catch (NumberFormatException ignored) {}
                    }

                    // FIX PRINCIPAL : typeCarburant ne peut jamais être null en base
                    TypeCarburant typeCarburant = parseTypeCarburant(typeCarburantStr);

                    boolean exists = geRepo.existsBySite(site);
                    GroupeElectrogene ge = exists
                            ? geRepo.findById(site).orElse(new GroupeElectrogene())
                            : new GroupeElectrogene();

                    ge.setSite(site);
                    ge.setTypeCarburant(typeCarburant);           // jamais null
                    ge.setPuissanceKVA(puissance);
                    ge.setTauxConsommationParHeure(tauxConso);
                    ge.setConsommationTotaleMaxParSemestre(consoMax);
                    ge.setTypeCarte(typeCarte);
                    ge.setNumeroCarte(numeroCarte);
                    ge.setDateExpiration(parseDate(dateExpStr));
                    ge.setCodePIN(codePIN);
                    ge.setCodePUK(codePUK);
                    ge.setUtilisateurRoc(utilisateurRoc);
                    if (zone != null) ge.setZone(zone);

                    // Ne pas écraser le prix carburant s'il est déjà défini
                    // (le prix est saisi manuellement, pas dans l'Excel)

                    try {
                        geRepo.save(ge);
                        if (exists) updated++; else imported++;
                        log.debug("Site '{}' {} avec succès", site, exists ? "mis à jour" : "importé");
                    } catch (Exception e) {
                        skipped++;
                        errors.add("Erreur ligne " + (row.getRowNum() + 1) + " (site=" + site + ") : " + e.getMessage());
                        log.error("Erreur import site '{}' : {}", site, e.getMessage());
                    }
                }
            }
        }

        log.info("Import terminé : {} créés, {} mis à jour, {} ignorés, {} erreurs",
                imported, updated, skipped, errors.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("updated",  updated);
        result.put("skipped",  skipped);
        result.put("errors",   errors);
        return result;
    }

    // ── Helpers cellules ─────────────────────────────────────────────────────

    private String getStringCell(Cell c) {
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> {
                String v = c.getStringCellValue().trim();
                v = v.replaceAll("[\\p{Cf}\\p{Zs}\\uFEFF\\u200B]", "").trim();
                yield v.isBlank() ? null : v;
            }
            case NUMERIC -> {
                // Retourner la valeur numérique sous forme de chaîne
                double d = c.getNumericCellValue();
                // Si c'est un entier (ex: 630, 400), éviter le ".0"
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            default -> null;
        };
    }

    /**
     * FIX : Lecture spéciale pour PIN/PUK qui sont stockés comme nombres dans Excel.
     * Ex : la valeur Excel 177 doit donner "0177" (padding avec zéros si nécessaire),
     * mais en pratique on stocke juste la valeur brute.
     */
    private String getStringCellForPin(Cell c) {
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) {
            long val = (long) c.getNumericCellValue();
            return String.valueOf(val);
        }
        return getStringCell(c);
    }

    private Double getDoubleCell(Cell c) {
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
        if (c.getCellType() == CellType.STRING) {
            String s = c.getStringCellValue().trim();
            if (s.equalsIgnoreCase("démonté") || s.isBlank()) return null;
            try { return Double.parseDouble(s); } catch (Exception e) { return null; }
        }
        return null;
    }

    /**
     * Parse une date au format "MM/yyyy" vers LocalDate (1er du mois).
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            dateStr = dateStr.replaceAll("[\\p{Cf}\\p{Zs}\\uFEFF\\u200B]", "").trim();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
            return java.time.YearMonth.parse(dateStr, fmt).atDay(1);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ex) {
                log.warn("Date invalide ignorée : '{}'", dateStr);
                return null;
            }
        }
    }

    /**
     * FIX IMPORTANT : typeCarburant retourne toujours une valeur non-null.
     * La valeur par défaut est GASOIL_ORDINAIRE.
     */
    private TypeCarburant parseTypeCarburant(String raw) {
        if (raw == null || raw.isBlank()) return TypeCarburant.GASOIL_ORDINAIRE;
        String s = raw.toUpperCase()
                .replaceAll("[\\p{Cf}\\s]+", " ")
                .trim();
        if (s.contains("ESSENCE"))                                    return TypeCarburant.ESSENCE;
        if (s.contains("SANS SOUFRE") || s.contains("SANS SOUFRE")) return TypeCarburant.GASOIL_SANS_SOUFRE;
        if (s.contains("SUPER SANS PLOMB"))                          return TypeCarburant.SUPER_SANS_PLOMB;
        return TypeCarburant.GASOIL_ORDINAIRE;
    }
}