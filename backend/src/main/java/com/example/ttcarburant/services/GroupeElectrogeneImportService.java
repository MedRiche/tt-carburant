// com.example.ttcarburant.services.GroupeElectrogeneImportService.java
package com.example.ttcarburant.services;

import com.example.ttcarburant.model.entity.GroupeElectrogene;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.model.enums.TypeCarburant;
import com.example.ttcarburant.repository.GroupeElectrogeneRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GroupeElectrogeneImportService {

    private final GroupeElectrogeneRepository geRepo;
    private final ZoneRepository zoneRepo;

    public GroupeElectrogeneImportService(GroupeElectrogeneRepository geRepo, ZoneRepository zoneRepo) {
        this.geRepo = geRepo;
        this.zoneRepo = zoneRepo;
    }

    /**
     * Détecte si la feuille est au format "DEUXIEME SEMESTRE" (carte Agilis avant le type carburant)
     * en inspectant la cellule de l'en-tête (ligne 1, index 0).
     *
     * Format PREMIER SEMESTRE (colonnes 0-22) :
     *   0=id, 1=Site, 2=TypeCarburant, 3=Puissance, 4=TauxConso, 5=ConsoMax,
     *   6..15=données semestrielles, 16=TypeCarte, 17=NuméroC, 18=DateExp, 19=PIN, 20=PUK, 21=UserROC
     *
     * Format DEUXIEME SEMESTRE (colonnes 0-22) :
     *   0=id, 1=Site, 2=TypeCarte, 3=NuméroCarte, 4=DateExp, 5=PIN, 6=PUK, 7=UserROC,
     *   8=TypeCarburant, 9=Puissance, 10=TauxConso, 11=ConsoMax,
     *   12..21=données semestrielles
     */
    private boolean isDeuxiemeSemestreFormat(Sheet sheet) {
        Row header = sheet.getRow(1);
        if (header == null) return false;
        String cell2 = getStringCell(header.getCell(2));
        // Dans le format S2, la colonne 2 contient "Type Carte", pas "Type Carburant"
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
            // Traiter toutes les feuilles du classeur
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
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
                        // Format DEUXIEME SEMESTRE
                        site            = getStringCell(row.getCell(1));
                        typeCarte       = getStringCell(row.getCell(2));
                        numeroCarte     = getStringCell(row.getCell(3));
                        dateExpStr      = getStringCell(row.getCell(4));
                        codePIN         = getStringCell(row.getCell(5));
                        codePUK         = getStringCell(row.getCell(6));
                        utilisateurRoc  = getStringCell(row.getCell(7));
                        typeCarburantStr= getStringCell(row.getCell(8));
                        puissanceStr    = getStringCell(row.getCell(9));
                        tauxConso       = getDoubleCell(row.getCell(10));
                        consoMax        = getDoubleCell(row.getCell(11));
                    } else {
                        // Format PREMIER SEMESTRE
                        site            = getStringCell(row.getCell(1));
                        typeCarburantStr= getStringCell(row.getCell(2));
                        puissanceStr    = getStringCell(row.getCell(3));
                        tauxConso       = getDoubleCell(row.getCell(4));
                        consoMax        = getDoubleCell(row.getCell(5));
                        typeCarte       = getStringCell(row.getCell(16));
                        numeroCarte     = getStringCell(row.getCell(17));
                        dateExpStr      = getStringCell(row.getCell(18));
                        codePIN         = getStringCell(row.getCell(19));
                        codePUK         = getStringCell(row.getCell(20));
                        utilisateurRoc  = getStringCell(row.getCell(21));
                    }

                    if (site == null || site.isBlank()) continue;

                    // Ignorer les sites "démonté" si la puissance l'indique
                    Double puissance = null;
                    if (puissanceStr != null && !puissanceStr.equalsIgnoreCase("démonté")) {
                        try { puissance = Double.parseDouble(puissanceStr.trim()); } catch (NumberFormatException ignored) {}
                    }

                    boolean exists = geRepo.existsBySite(site);
                    GroupeElectrogene ge = exists ? geRepo.findById(site).orElse(new GroupeElectrogene()) : new GroupeElectrogene();

                    ge.setSite(site);
                    ge.setTypeCarburant(parseTypeCarburant(typeCarburantStr));
                    ge.setPuissanceKVA(puissance);
                    ge.setTauxConsommationParHeure(tauxConso);
                    ge.setConsommationTotaleMaxParSemestre(consoMax);
                    ge.setTypeCarte(typeCarte);
                    ge.setNumeroCarte(numeroCarte);
                    ge.setDateExpiration(parseDate(dateExpStr));
                    ge.setCodePIN(codePIN != null ? String.valueOf(codePIN) : null);
                    ge.setCodePUK(codePUK != null ? String.valueOf(codePUK) : null);
                    ge.setUtilisateurRoc(utilisateurRoc);
                    if (zone != null) ge.setZone(zone);

                    // Ne pas écraser le prix carburant si déjà défini
                    if (ge.getPrixCarburant() == null) {
                        ge.setPrixCarburant(null); // Sera saisi manuellement
                    }

                    geRepo.save(ge);
                    if (exists) updated++; else imported++;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    private String getStringCell(Cell c) {
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> {
                String v = c.getStringCellValue().trim();
                // Nettoyer les caractères invisibles (zero-width no-break space, etc.)
                v = v.replaceAll("[\\p{Cf}\\p{Zs}\\uFEFF\\u200B]", "").trim();
                yield v.isBlank() ? null : v;
            }
            case NUMERIC -> {
                long lv = (long) c.getNumericCellValue();
                yield String.valueOf(lv);
            }
            default -> null;
        };
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
     * Parse une date au format "MM/yyyy" (ex: "08/2028") vers LocalDate (1er du mois).
     * CORRIGÉ : le format original contenait déjà "MM/yyyy", pas besoin d'ajouter "01/".
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            // Nettoyer les caractères invisibles
            dateStr = dateStr.replaceAll("[\\p{Cf}\\p{Zs}\\uFEFF\\u200B]", "").trim();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
            // YearMonth -> premier jour du mois
            return java.time.YearMonth.parse(dateStr, fmt).atDay(1);
        } catch (Exception e) {
            // Tenter format dd/MM/yyyy en fallback
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private TypeCarburant parseTypeCarburant(String raw) {
        if (raw == null) return TypeCarburant.GASOIL_ORDINAIRE;
        String s = raw.toUpperCase().replaceAll("[\\p{Cf}\\s]+", " ").trim();
        if (s.contains("ESSENCE")) return TypeCarburant.ESSENCE;
        if (s.contains("SANS SOUFFRE") || s.contains("SANS SOUFRE")) return TypeCarburant.GASOIL_SANS_SOUFRE;
        if (s.contains("SUPER SANS PLOMB")) return TypeCarburant.SUPER_SANS_PLOMB;
        return TypeCarburant.GASOIL_ORDINAIRE;
    }
}