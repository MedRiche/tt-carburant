package com.example.ttcarburant.services;

import com.example.ttcarburant.model.entity.DetailMaintenance;
import com.example.ttcarburant.model.entity.Maintenance;
import com.example.ttcarburant.model.enums.TypeDetailMaintenance;
import com.example.ttcarburant.repository.MaintenanceRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MaintenanceExportService {

    private final MaintenanceRepository maintenanceRepo;

    public MaintenanceExportService(MaintenanceRepository maintenanceRepo) {
        this.maintenanceRepo = maintenanceRepo;
    }

    public byte[] exporterExcel(Long zoneId, String statutStr) throws Exception {
        List<Maintenance> data;
        if (zoneId != null) {
            data = maintenanceRepo.findByZoneId(zoneId);
        } else {
            data = maintenanceRepo.findAll();
        }

        XSSFWorkbook wb = new XSSFWorkbook();

        // ── Feuille 1 : Global Vehicle List ──────────────────────────────
        XSSFSheet globalSheet = wb.createSheet("Global Vehicle List");
        globalSheet.setColumnWidth(0, 16 * 256);
        globalSheet.setColumnWidth(1, 14 * 256);
        globalSheet.setColumnWidth(2, 20 * 256);
        globalSheet.setColumnWidth(3, 30 * 256);

        CellStyle hdrStyle = createHeaderStyle(wb);
        CellStyle dataStyle = createDataStyle(wb);
        CellStyle numStyle  = createNumStyle(wb);
        CellStyle titleStyle = createTitleStyle(wb);

        Row title = globalSheet.createRow(0);
        title.setHeightInPoints(24);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("LISTE GLOBALE MAINTENANCE — TUNISIE TELECOM");
        titleCell.setCellStyle(titleStyle);
        globalSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        Row hdr = globalSheet.createRow(2);
        String[] headers = {"Véhicule ID", "Total HTVA (DT)", "Brand(s)", "Zone"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = hdr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hdrStyle);
        }

        // Grouper par véhicule
        Map<String, List<Maintenance>> byVehicule = data.stream()
                .collect(Collectors.groupingBy(m -> m.getVehicule().getMatricule()));

        List<Map.Entry<String, List<Maintenance>>> sorted = new ArrayList<>(byVehicule.entrySet());
        sorted.sort((a, b) -> {
            double totalA = a.getValue().stream().mapToDouble(Maintenance::getCoutTotalHtva).sum();
            double totalB = b.getValue().stream().mapToDouble(Maintenance::getCoutTotalHtva).sum();
            return Double.compare(totalB, totalA);
        });

        int row = 3;
        for (Map.Entry<String, List<Maintenance>> entry : sorted) {
            double total = entry.getValue().stream().mapToDouble(Maintenance::getCoutTotalHtva).sum();
            String brands = entry.getValue().stream()
                    .flatMap(m -> m.getDetails().stream())
                    .map(DetailMaintenance::getMarque)
                    .filter(Objects::nonNull)
                    .distinct().sorted()
                    .collect(Collectors.joining(", "));
            String zone = entry.getValue().get(0).getVehicule().getZone() != null
                    ? entry.getValue().get(0).getVehicule().getZone().getNom() : "—";

            Row r = globalSheet.createRow(row++);
            setCell(r, 0, entry.getKey(), dataStyle);
            setCellNum(r, 1, Math.round(total * 1000.0) / 1000.0, numStyle);
            setCell(r, 2, brands, dataStyle);
            setCell(r, 3, zone, dataStyle);
        }

        // ── Feuille 2 : Détails par véhicule ─────────────────────────────
        for (Map.Entry<String, List<Maintenance>> entry : sorted) {
            String mat = entry.getKey();
            String safeName = mat.replace(":", "").replace("/", "-");
            if (safeName.length() > 31) safeName = safeName.substring(0, 31);

            XSSFSheet detSheet = wb.createSheet(safeName);
            detSheet.setColumnWidth(0, 12 * 256);
            detSheet.setColumnWidth(1, 16 * 256);
            detSheet.setColumnWidth(2, 14 * 256);
            detSheet.setColumnWidth(3, 12 * 256);
            detSheet.setColumnWidth(4, 35 * 256);
            detSheet.setColumnWidth(5, 8 * 256);
            detSheet.setColumnWidth(6, 12 * 256);
            detSheet.setColumnWidth(7, 14 * 256);

            double totalHtva = entry.getValue().stream().mapToDouble(Maintenance::getCoutTotalHtva).sum();

            Row r0 = detSheet.createRow(0);
            r0.setHeightInPoints(20);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("Total HTVA: " + Math.round(totalHtva * 1000.0) / 1000.0);
            c0.setCellStyle(titleStyle);
            detSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            int detRow = 2;

            // Main d'œuvre section
            Row moLabel = detSheet.createRow(detRow++);
            Cell moCell = moLabel.createCell(0);
            moCell.setCellValue("Main d'œuvre");
            moCell.setCellStyle(createSectionStyle(wb, "5B9BD5"));

            Row moHdr = detSheet.createRow(detRow++);
            String[] cols = {"N° Doss", "Véhicule", "Marque", "N°", "Désignation", "Qté", "Montant", "Total HTVA"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = moHdr.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(hdrStyle);
            }

            for (Maintenance m : entry.getValue()) {
                for (DetailMaintenance d : m.getDetails()) {
                    if (d.getType() != TypeDetailMaintenance.MAIN_D_OEUVRE) continue;
                    Row dr = detSheet.createRow(detRow++);
                    setCell(dr, 0, m.getNumeroDossier(), dataStyle);
                    setCell(dr, 1, mat, dataStyle);
                    setCell(dr, 2, d.getMarque() != null ? d.getMarque() : "—", dataStyle);
                    setCell(dr, 3, d.getNumero() != null ? d.getNumero() : "—", dataStyle);
                    setCell(dr, 4, d.getDesignation(), dataStyle);
                    setCellNum(dr, 5, d.getQuantite(), numStyle);
                    setCellNum(dr, 6, d.getMontantUnitaire(), numStyle);
                    setCellNum(dr, 7, d.getTotalHtva(), numStyle);
                }
            }

            detRow++;

            // Pièces section
            Row pieceLabel = detSheet.createRow(detRow++);
            Cell pieceCell = pieceLabel.createCell(0);
            pieceCell.setCellValue("Pièces");
            pieceCell.setCellStyle(createSectionStyle(wb, "ED7D31"));

            Row pieceHdr = detSheet.createRow(detRow++);
            for (int i = 0; i < cols.length; i++) {
                Cell c = pieceHdr.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(hdrStyle);
            }

            for (Maintenance m : entry.getValue()) {
                for (DetailMaintenance d : m.getDetails()) {
                    if (d.getType() != TypeDetailMaintenance.PIECE) continue;
                    Row dr = detSheet.createRow(detRow++);
                    setCell(dr, 0, m.getNumeroDossier(), dataStyle);
                    setCell(dr, 1, mat, dataStyle);
                    setCell(dr, 2, d.getMarque() != null ? d.getMarque() : "—", dataStyle);
                    setCell(dr, 3, d.getNumeroPiece() != null ? d.getNumeroPiece() : "—", dataStyle);
                    setCell(dr, 4, d.getDesignation(), dataStyle);
                    setCellNum(dr, 5, d.getQuantite(), numStyle);
                    setCellNum(dr, 6, d.getMontantUnitaire(), numStyle);
                    setCellNum(dr, 7, d.getTotalHtva(), numStyle);
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ── Styles ────────────────────────────────────────────────────────────

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 13);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)31,(byte)73,(byte)125}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setIndention((short) 1);
        return s;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)68,(byte)114,(byte)196}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        return s;
    }

    private CellStyle createSectionStyle(XSSFWorkbook wb, String hexColor) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        int r = Integer.parseInt(hexColor.substring(0, 2), 16);
        int g = Integer.parseInt(hexColor.substring(2, 4), 16);
        int b = Integer.parseInt(hexColor.substring(4, 6), 16);
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)r,(byte)g,(byte)b}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setIndention((short) 1);
        return s;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short) 10);
        s.setFont(f);
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

    private void setBorder(CellStyle s, BorderStyle bs) {
        s.setBorderTop(bs); s.setBorderBottom(bs);
        s.setBorderLeft(bs); s.setBorderRight(bs);
    }

    private void setCell(Row r, int col, String val, CellStyle s) {
        Cell c = r.createCell(col); c.setCellValue(val != null ? val : "—"); c.setCellStyle(s);
    }

    private void setCellNum(Row r, int col, double val, CellStyle s) {
        Cell c = r.createCell(col); c.setCellValue(val); c.setCellStyle(s);
    }
}