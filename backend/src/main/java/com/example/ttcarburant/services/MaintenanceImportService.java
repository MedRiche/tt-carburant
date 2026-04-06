package com.example.ttcarburant.services;

import com.example.ttcarburant.model.entity.DetailMaintenance;
import com.example.ttcarburant.model.entity.Maintenance;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.enums.StatutMaintenance;
import com.example.ttcarburant.model.enums.TypeDetailMaintenance;
import com.example.ttcarburant.model.enums.TypeIntervention;
import com.example.ttcarburant.repository.MaintenanceRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

/**
 * Importe les dossiers de maintenance depuis le fichier Dataset_Complet.xlsx.
 *
 * Structure attendue :
 *  - Feuille "Global Vehicle List" : liste des véhicules avec totalHTVA et Brand(s)
 *  - Feuilles "V_17-XXXXXX"        : détail par véhicule (Main d'œuvre + Pièces)
 *    Format colonnes : N° Doss | Véhicule | Marque | N° | Designation | Qté | Montant | Total HTVA
 */
@Service
public class MaintenanceImportService {

    private final MaintenanceRepository maintenanceRepo;
    private final VehiculeRepository vehiculeRepo;

    public MaintenanceImportService(MaintenanceRepository maintenanceRepo,
                                    VehiculeRepository vehiculeRepo) {
        this.maintenanceRepo = maintenanceRepo;
        this.vehiculeRepo = vehiculeRepo;
    }

    @Transactional
    public Map<String, Object> importerDataset(MultipartFile file) throws Exception {
        int imported = 0;
        int skipped  = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            // Itérer sur les feuilles "V_17-XXXXXX"
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                if (!sheetName.startsWith("V_17-")) continue;

                String matricule = sheetName.substring(2); // "17-XXXXXX"

                try {
                    Optional<Vehicule> vOpt = vehiculeRepo.findById(matricule);
                    if (vOpt.isEmpty()) {
                        // Créer le véhicule avec des infos minimales si inexistant
                        errors.add("Véhicule " + matricule + " non trouvé en BDD — ignoré");
                        skipped++;
                        continue;
                    }
                    Vehicule vehicule = vOpt.get();

                    // Lire le total HTVA depuis la ligne "Total HTVA: XX"
                    double totalHtvaGlobal = lireTotalHtva(sheet);

                    // Parser les détails Main d'œuvre et Pièces
                    ParsedSheet parsed = parserFeuille(sheet, matricule);
                    if (parsed.details.isEmpty()) { skipped++; continue; }

                    // Grouper par N° Dossier pour créer un Maintenance par dossier
                    Map<String, List<ParsedDetail>> parDossier = new LinkedHashMap<>();
                    for (ParsedDetail d : parsed.details) {
                        parDossier.computeIfAbsent(d.numeroDossier, k -> new ArrayList<>()).add(d);
                    }

                    for (Map.Entry<String, List<ParsedDetail>> entry : parDossier.entrySet()) {
                        String numeroDossier = entry.getKey();
                        List<ParsedDetail> detailsList = entry.getValue();

                        // Vérifier doublon
                        if (maintenanceRepo.findByNumeroDossierAndVehicule_Matricule(
                                numeroDossier, matricule).isPresent()) {
                            skipped++;
                            continue;
                        }

                        Maintenance m = new Maintenance();
                        m.setNumeroDossier(numeroDossier);
                        m.setVehicule(vehicule);
                        m.setDateIntervention(LocalDate.now()); // date import
                        m.setTypeIntervention(TypeIntervention.CORRECTIVE);
                        m.setStatut(StatutMaintenance.TERMINEE);
                        m.setDescription("Importé depuis Dataset_Complet.xlsx");
                        m.setCreePar("import");

                        for (ParsedDetail pd : detailsList) {
                            DetailMaintenance d = new DetailMaintenance();
                            d.setMaintenance(m);
                            d.setType(pd.type);
                            d.setNumeroDossier(pd.numeroDossier);
                            d.setMarque(pd.marque);
                            d.setNumero(pd.numero);
                            d.setNumeroPiece(pd.numeroPiece);
                            d.setDesignation(pd.designation);
                            d.setQuantite(pd.quantite);
                            d.setMontantUnitaire(pd.montantUnitaire);
                            d.calculerMontant();
                            m.getDetails().add(d);
                        }
                        m.recalculerTotal();
                        maintenanceRepo.save(m);
                        imported++;
                    }
                } catch (Exception e) {
                    errors.add("Erreur feuille " + sheetName + ": " + e.getMessage());
                    skipped++;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    // ── Parsing ───────────────────────────────────────────────────────────

    private double lireTotalHtva(Sheet sheet) {
        for (Row row : sheet) {
            Cell c = row.getCell(0);
            if (c != null && c.getCellType() == CellType.STRING) {
                String val = c.getStringCellValue().trim();
                if (val.startsWith("Total HTVA:")) {
                    try {
                        return Double.parseDouble(val.replace("Total HTVA:", "").trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 0;
    }

    private ParsedSheet parserFeuille(Sheet sheet, String matricule) {
        ParsedSheet result = new ParsedSheet();
        TypeDetailMaintenance currentType = null;
        boolean inDataSection = false;

        for (Row row : sheet) {
            if (row == null) continue;
            Cell c0 = row.getCell(0);
            if (c0 == null) continue;

            String val0 = getCellString(c0).trim().toLowerCase();

            // Détecter les sections
            if (val0.contains("main d") || val0.contains("main d'œuvre") || val0.contains("main d'oeuvre")) {
                currentType = TypeDetailMaintenance.MAIN_D_OEUVRE;
                inDataSection = false;
                continue;
            }
            if (val0.equals("pièce") || val0.equals("pieces") || val0.equals("pièces") || val0.equals("piece")) {
                currentType = TypeDetailMaintenance.PIECE;
                inDataSection = false;
                continue;
            }
            // Ligne d'en-tête
            if (val0.equals("n° doss") || val0.equals("n doss")) {
                inDataSection = true;
                continue;
            }

            if (!inDataSection || currentType == null) continue;

            // Ligne de données
            try {
                ParsedDetail d = new ParsedDetail();
                d.type = currentType;

                // Col 0: N° Doss
                d.numeroDossier = getCellString(row.getCell(0));
                if (d.numeroDossier == null || d.numeroDossier.isBlank()) continue;

                // Col 1: Véhicule (on ignore, on prend le matricule de la feuille)
                // Col 2: Marque
                d.marque = getCellString(row.getCell(2));
                // Col 3: N° (prestation ou pièce)
                String num = getCellString(row.getCell(3));
                if (currentType == TypeDetailMaintenance.MAIN_D_OEUVRE) {
                    d.numero = num;
                } else {
                    d.numeroPiece = num;
                }
                // Col 4: Désignation
                d.designation = getCellString(row.getCell(4));
                if (d.designation == null || d.designation.isBlank()) continue;

                // Col 5: Qté
                d.quantite = (int) getCellNumeric(row.getCell(5), 1);

                // Col 6: Montant unitaire
                d.montantUnitaire = getCellNumeric(row.getCell(6), 0);

                // Col 7: Total HTVA
                double htva = getCellNumeric(row.getCell(7), 0);
                if (htva == 0 && d.montantUnitaire > 0) {
                    htva = d.quantite * d.montantUnitaire;
                }

                result.details.add(d);

            } catch (Exception ignored) {}
        }
        return result;
    }

    private String getCellString(Cell c) {
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            case FORMULA -> {
                try { yield c.getStringCellValue().trim(); }
                catch (Exception e) { yield String.valueOf(c.getNumericCellValue()); }
            }
            default -> null;
        };
    }

    private double getCellNumeric(Cell c, double def) {
        if (c == null) return def;
        return switch (c.getCellType()) {
            case NUMERIC -> c.getNumericCellValue();
            case STRING  -> {
                try { yield Double.parseDouble(c.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield def; }
            }
            default -> def;
        };
    }

    // ── Structures internes ───────────────────────────────────────────────

    private static class ParsedSheet {
        final List<ParsedDetail> details = new ArrayList<>();
    }

    private static class ParsedDetail {
        TypeDetailMaintenance type;
        String numeroDossier;
        String marque;
        String numero;
        String numeroPiece;
        String designation;
        int quantite = 1;
        double montantUnitaire;
    }
}