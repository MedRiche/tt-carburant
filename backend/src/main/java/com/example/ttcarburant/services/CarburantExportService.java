package com.example.ttcarburant.services;

import com.example.ttcarburant.model.entity.GestionCarburantVehicule;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.repository.CarburantVehiculeRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CarburantExportService {

    private static final String[] MOIS_LABELS = {
            "", "Janvier","Février","Mars","Avril","Mai","Juin",
            "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
    };

    private final CarburantVehiculeRepository carburantRepo;
    private final VehiculeRepository vehiculeRepo;
    private final ZoneRepository zoneRepo;

    public CarburantExportService(CarburantVehiculeRepository carburantRepo,
                                  VehiculeRepository vehiculeRepo,
                                  ZoneRepository zoneRepo) {
        this.carburantRepo = carburantRepo;
        this.vehiculeRepo  = vehiculeRepo;
        this.zoneRepo      = zoneRepo;
    }

    // ── Export mensuel (format DAF 2026) ────────────────────────

    public byte[] exportMensuelExcel(int annee, int mois, Long zoneId) throws Exception {
        List<GestionCarburantVehicule> data = (zoneId != null)
                ? carburantRepo.findByZoneAndPeriode(zoneId, annee, mois)
                : carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(annee, mois);

        XSSFWorkbook wb = new XSSFWorkbook();
        String sheetName = MOIS_LABELS[mois] + " " + annee;
        XSSFSheet sheet = wb.createSheet(sheetName);

        // Styles
        CellStyle titleStyle    = createTitleStyle(wb);
        CellStyle headerStyle   = createHeaderStyle(wb);
        CellStyle dataStyle     = createDataStyle(wb);
        CellStyle numStyle      = createNumStyle(wb);
        CellStyle alertStyle    = createAlertStyle(wb);
        CellStyle calcStyle     = createCalcStyle(wb);
        CellStyle totalStyle    = createTotalStyle(wb);

        // Largeurs colonnes
        int[] widths = {18,22,12,12,16,16,14,14,14,16,16,16,14};
        for (int i = 0; i < widths.length; i++)
            sheet.setColumnWidth(i, widths[i] * 256);

        int row = 0;

        // Titre principal
        Row titleRow = sheet.createRow(row++);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("GESTION CARBURANT VÉHICULES — " + sheetName.toUpperCase());
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

        // Sous-titre zone
        if (zoneId != null) {
            Row zoneRow = sheet.createRow(row++);
            String zoneNom = zoneRepo.findById(zoneId).map(z -> z.getNom()).orElse("Zone " + zoneId);
            Cell zoneCell = zoneRow.createCell(0);
            zoneCell.setCellValue("Zone : " + zoneNom);
            zoneCell.setCellStyle(createSubTitleStyle(wb));
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 12));
        }

        row++; // Ligne vide

        // En-têtes colonnes
        Row hdr = sheet.createRow(row++);
        hdr.setHeightInPoints(36);
        String[] headers = {
                "Matricule", "Marque / Modèle", "Type Carb.", "Prix (DT/L)",
                "Index Démarrage", "Index Fin Mois", "Distance (km)",
                "Total Ravit. (L)", "Qté Restante (L)", "% Conso",
                "Carb. Demandé (DT)", "Budget (DT)", "Alerte"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell c = hdr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        // Données
        int dataStartRow = row + 1;
        for (GestionCarburantVehicule g : data) {
            Row r = sheet.createRow(row++);
            r.setHeightInPoints(18);
            setCell(r, 0, g.getVehicule().getMatricule(), dataStyle);
            setCell(r, 1, g.getVehicule().getMarqueModele(), dataStyle);
            setCell(r, 2, g.getVehicule().getTypeCarburant().name(), dataStyle);
            setCellNum(r, 3, g.getVehicule().getPrixCarburant(), numStyle);
            setCellNum(r, 4, g.getIndexDemarrageMois(), numStyle);
            setCellNum(r, 5, g.getIndexFinMois(), numStyle);
            setCellNum(r, 6, g.getDistanceParcourue(), calcStyle);
            setCellNum(r, 7, g.getTotalRavitaillementLitres(), calcStyle);
            setCellNum(r, 8, g.getQuantiteRestanteReservoir(), calcStyle);
            setCellNum(r, 9, g.getPourcentageConsommation(), calcStyle);
            setCellNum(r, 10, g.getCarburantDemandeDinars(), calcStyle);
            setCellNum(r, 11, g.getVehicule().getCoutDuMois(), numStyle);
            // Alerte budget
            Cell alertCell = r.createCell(12);
            if (g.isBudgetDepasse()) {
                alertCell.setCellValue("⚠️ +" + String.format("%.3f", g.getDepassementMontant()) + " DT");
                alertCell.setCellStyle(alertStyle);
            } else {
                alertCell.setCellValue("✓ OK");
                alertCell.setCellStyle(dataStyle);
            }
        }

        // Ligne TOTAUX
        if (!data.isEmpty()) {
            Row totRow = sheet.createRow(row++);
            totRow.setHeightInPoints(20);
            Cell totLabel = totRow.createCell(0);
            totLabel.setCellValue("TOTAUX");
            totLabel.setCellStyle(totalStyle);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 5));

            int lastDataRow = row - 1;
            int firstDataRow = dataStartRow;
            // Total distance
            Cell totDist = totRow.createCell(6);
            totDist.setCellFormula("SUM(G" + firstDataRow + ":G" + lastDataRow + ")");
            totDist.setCellStyle(totalStyle);
            // Total litres
            Cell totLit = totRow.createCell(7);
            totLit.setCellFormula("SUM(H" + firstDataRow + ":H" + lastDataRow + ")");
            totLit.setCellStyle(totalStyle);
            // Total demandé
            Cell totDem = totRow.createCell(10);
            totDem.setCellFormula("SUM(K" + firstDataRow + ":K" + lastDataRow + ")");
            totDem.setCellStyle(totalStyle);
        }

        // Ligne vide + note formules DAF
        row++;
        Row noteRow = sheet.createRow(row);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue(
                "Formules DAF 2026 — (1) Total L = (Ravit. préc. + Restant préc.) / Prix  " +
                        "(2) Qté rest. = Restant préc. / Prix  " +
                        "(3) Dist. = Index fin − Index démarrage  " +
                        "(4) % = (Total L − Qté rest.) / Distance  " +
                        "(5) Carb. DT = Budget − Restant préc."
        );
        noteCell.setCellStyle(createNoteStyle(wb));
        sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 12));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ── Export annuel (recap 12 mois) ────────────────────────────

    public byte[] exportAnnuelExcel(int annee, Long zoneId) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();

        CellStyle titleStyle  = createTitleStyle(wb);
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dataStyle   = createDataStyle(wb);
        CellStyle numStyle    = createNumStyle(wb);
        CellStyle calcStyle   = createCalcStyle(wb);
        CellStyle totalStyle  = createTotalStyle(wb);
        CellStyle monthStyle  = createMonthStyle(wb);

        // Une feuille par véhicule (ou on groupe tout sur une feuille récap)
        // Ici : une feuille récap globale + une feuille par mois ayant des données
        XSSFSheet recap = wb.createSheet("Récap Annuel " + annee);
        recap.setColumnWidth(0, 18 * 256);
        recap.setColumnWidth(1, 22 * 256);
        for (int i = 2; i <= 14; i++) recap.setColumnWidth(i, 13 * 256);

        int row = 0;
        Row titleRow = recap.createRow(row++);
        titleRow.setHeightInPoints(28);
        Cell tc = titleRow.createCell(0);
        tc.setCellValue("RÉCAPITULATIF ANNUEL CARBURANT — " + annee);
        tc.setCellStyle(titleStyle);
        recap.addMergedRegion(new CellRangeAddress(0, 0, 0, 14));

        row++;

        // En-tête : Matricule | Marque | Jan | Fév | … | Déc | TOTAL km | TOTAL L | TOTAL DT
        Row hdr = recap.createRow(row++);
        hdr.setHeightInPoints(32);
        Cell h0 = hdr.createCell(0); h0.setCellValue("Matricule");    h0.setCellStyle(headerStyle);
        Cell h1 = hdr.createCell(1); h1.setCellValue("Marque");       h1.setCellStyle(headerStyle);
        for (int m = 1; m <= 12; m++) {
            Cell hm = hdr.createCell(m + 1);
            hm.setCellValue(MOIS_LABELS[m].substring(0, 3));
            hm.setCellStyle(monthStyle);
        }
        Cell htk = hdr.createCell(14); htk.setCellValue("Total km");  htk.setCellStyle(totalStyle);
        Cell htl = hdr.createCell(15); htl.setCellValue("Total L");   htl.setCellStyle(totalStyle);
        Cell htd = hdr.createCell(16); htd.setCellValue("Total DT");  htd.setCellStyle(totalStyle);

        // Récupérer tous les véhicules ayant des données cette année
        List<GestionCarburantVehicule> allData = (zoneId != null)
                ? carburantRepo.findByZoneAndAnnee(zoneId, annee)
                : getAllAnneeData(annee);

        // Grouper par véhicule
        Map<String, List<GestionCarburantVehicule>> byVehicule = allData.stream()
                .collect(Collectors.groupingBy(g -> g.getVehicule().getMatricule()));

        for (Map.Entry<String, List<GestionCarburantVehicule>> entry : byVehicule.entrySet()) {
            Row r = recap.createRow(row++);
            r.setHeightInPoints(18);
            List<GestionCarburantVehicule> vehiculeData = entry.getValue();
            GestionCarburantVehicule first = vehiculeData.get(0);

            setCell(r, 0, first.getVehicule().getMatricule(), dataStyle);
            setCell(r, 1, first.getVehicule().getMarqueModele(), dataStyle);

            double totalKm = 0, totalL = 0, totalDT = 0;

            for (int m = 1; m <= 12; m++) {
                final int fm = m;
                Optional<GestionCarburantVehicule> moisOpt = vehiculeData.stream()
                        .filter(g -> g.getMois() == fm).findFirst();
                if (moisOpt.isPresent()) {
                    GestionCarburantVehicule g = moisOpt.get();
                    setCellNum(r, m + 1, g.getDistanceParcourue(), calcStyle);
                    totalKm += g.getDistanceParcourue();
                    totalL  += g.getTotalRavitaillementLitres();
                    totalDT += g.getCarburantDemandeDinars();
                } else {
                    Cell empty = r.createCell(m + 1);
                    empty.setCellValue("—");
                    empty.setCellStyle(dataStyle);
                }
            }
            setCellNum(r, 14, totalKm, totalStyle);
            setCellNum(r, 15, totalL,  totalStyle);
            setCellNum(r, 16, totalDT, totalStyle);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    private List<GestionCarburantVehicule> getAllAnneeData(int annee) {
        // Récupérer pour chaque mois
        List<GestionCarburantVehicule> all = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            all.addAll(carburantRepo.findByAnneeAndMoisOrderByVehicule_Matricule(annee, m));
        }
        return all;
    }

    // ── Helpers styles ───────────────────────────────────────────

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)12,(byte)55,(byte)132}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createSubTitleStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)210,(byte)225,(byte)255}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setIndention((short)1);
        return s;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)31,(byte)73,(byte)125}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setBorder(s, BorderStyle.THIN);
        return s;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        return s;
    }

    private CellStyle createNumStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.000"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        setBorder(s, BorderStyle.THIN);
        return s;
    }

    private CellStyle createCalcStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(new byte[]{(byte)0,(byte)97,(byte)0}, null));
        s.setFont(f);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.000"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)235,(byte)255,(byte)235}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(s, BorderStyle.THIN);
        return s;
    }

    private CellStyle createAlertStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(new byte[]{(byte)156,(byte)0,(byte)6}, null));
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255,(byte)235,(byte)235}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        return s;
    }

    private CellStyle createTotalStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.000"));
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)198,(byte)224,(byte)180}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.RIGHT);
        setBorder(s, BorderStyle.MEDIUM);
        return s;
    }

    private CellStyle createMonthStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 9);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)68,(byte)114,(byte)196}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        return s;
    }

    private CellStyle createNoteStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setItalic(true); f.setFontHeightInPoints((short) 9);
        f.setColor(new XSSFColor(new byte[]{(byte)89,(byte)89,(byte)89}, null));
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)242,(byte)242,(byte)242}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setIndention((short)1);
        return s;
    }

    private void setBorder(CellStyle s, BorderStyle bs) {
        s.setBorderTop(bs); s.setBorderBottom(bs);
        s.setBorderLeft(bs); s.setBorderRight(bs);
    }

    private void setCell(Row r, int col, String val, CellStyle s) {
        Cell c = r.createCell(col); c.setCellValue(val); c.setCellStyle(s);
    }

    private void setCellNum(Row r, int col, double val, CellStyle s) {
        Cell c = r.createCell(col); c.setCellValue(val); c.setCellStyle(s);
    }
}