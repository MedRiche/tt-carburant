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
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                String site = getStringCell(row.getCell(1));
                if (site == null || site.isBlank()) continue;

                boolean exists = geRepo.existsBySite(site);
                GroupeElectrogene ge = exists ? geRepo.findById(site).get() : new GroupeElectrogene();
                ge.setSite(site);
                ge.setTypeCarburant(parseTypeCarburant(getStringCell(row.getCell(2))));
                ge.setPuissanceKVA(getDoubleCell(row.getCell(3)));
                ge.setTauxConsommationParHeure(getDoubleCell(row.getCell(4)));
                ge.setConsommationTotaleMaxParSemestre(getDoubleCell(row.getCell(5)));
                ge.setTypeCarte(getStringCell(row.getCell(18)));
                ge.setNumeroCarte(getStringCell(row.getCell(19)));
                ge.setDateExpiration(parseDate(getStringCell(row.getCell(20))));
                ge.setCodePIN(getStringCell(row.getCell(21)));
                ge.setCodePUK(getStringCell(row.getCell(22)));
                ge.setUtilisateurRoc(getStringCell(row.getCell(23)));
                if (zone != null) ge.setZone(zone);
                ge.setPrixCarburant(1.0); // default, can be updated later

                geRepo.save(ge);
                if (exists) updated++; else imported++;
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
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            default -> null;
        };
    }

    private Double getDoubleCell(Cell c) {
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
        if (c.getCellType() == CellType.STRING) {
            try { return Double.parseDouble(c.getStringCellValue().trim()); } catch (Exception e) { return null; }
        }
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
            return LocalDate.parse("01/" + dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) { return null; }
    }

    private TypeCarburant parseTypeCarburant(String raw) {
        if (raw == null) return TypeCarburant.GASOIL_ORDINAIRE;
        String s = raw.toUpperCase();
        if (s.contains("ESSENCE")) return TypeCarburant.ESSENCE;
        if (s.contains("SANS SOUFFRE")) return TypeCarburant.GASOIL_SANS_SOUFRE;
        if (s.contains("SUPER SANS PLOMB")) return TypeCarburant.SUPER_SANS_PLOMB;
        return TypeCarburant.GASOIL_ORDINAIRE;
    }
}