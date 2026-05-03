package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.Maintenance.DetailMaintenanceDto;
import com.example.ttcarburant.dto.Maintenance.MaintenanceDto;
import com.example.ttcarburant.dto.Maintenance.MaintenanceRequest;
import com.example.ttcarburant.model.entity.*;
import com.example.ttcarburant.model.enums.StatutMaintenance;
import com.example.ttcarburant.model.enums.TypeDetailMaintenance;
import com.example.ttcarburant.model.enums.TypeIntervention;
import com.example.ttcarburant.repository.*;
import com.example.ttcarburant.services.MaintenanceService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Étape 5 — Technicien : Consulter et gérer les maintenances de ses zones.
 * Routes :
 *   GET    /api/technicien/maintenances                → toutes les maintenances des zones
 *   GET    /api/technicien/maintenances/{id}           → détail d'un dossier
 *   GET    /api/technicien/maintenances/vehicule/{mat} → par véhicule
 *   GET    /api/technicien/maintenances/statut/{s}     → par statut
 *   GET    /api/technicien/maintenances/type/{t}       → par type
 *   POST   /api/technicien/maintenances                → créer un dossier
 *   PUT    /api/technicien/maintenances/{id}           → modifier
 *   DELETE /api/technicien/maintenances/{id}           → supprimer
 *   POST   /api/technicien/maintenances/{id}/details   → ajouter un détail
 *   DELETE /api/technicien/maintenances/{mid}/details/{did} → supprimer un détail
 *   GET    /api/technicien/maintenances/dashboard      → tableau de bord
 *   GET    /api/technicien/maintenances/export/excel   → export Excel global
 *   GET    /api/technicien/maintenances/{id}/export/excel → export Excel d'un dossier
 */
@RestController
@RequestMapping("/api/technicien/maintenances")
@PreAuthorize("hasRole('TECHNICIEN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class TechnicienMaintenanceController {

    private static final byte[] COLOR_TITLE_BG  = {(byte)0x1F,(byte)0x49,(byte)0x7D};
    private static final byte[] COLOR_HEADER_BG = {(byte)0x2E,(byte)0x75,(byte)0xB6};
    private static final byte[] COLOR_MO_BG     = {(byte)0xD6,(byte)0xE4,(byte)0xF0};
    private static final byte[] COLOR_PIECE_BG  = {(byte)0xFD,(byte)0xE8,(byte)0xD3};
    private static final byte[] COLOR_TOTAL_BG  = {(byte)0xC6,(byte)0xE0,(byte)0xB4};
    private static final byte[] COLOR_WARN_BG   = {(byte)0xFF,(byte)0xEB,(byte)0xEB};

    private final MaintenanceService maintenanceService;
    private final UtilisateurRepository utilisateurRepository;
    private final AffectationUtilisateurZoneRepository affectationRepository;
    private final VehiculeRepository vehiculeRepository;
    private final MaintenanceRepository maintenanceRepository;

    public TechnicienMaintenanceController(
            MaintenanceService maintenanceService,
            UtilisateurRepository utilisateurRepository,
            AffectationUtilisateurZoneRepository affectationRepository,
            VehiculeRepository vehiculeRepository,
            MaintenanceRepository maintenanceRepository) {
        this.maintenanceService    = maintenanceService;
        this.utilisateurRepository = utilisateurRepository;
        this.affectationRepository = affectationRepository;
        this.vehiculeRepository    = vehiculeRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    // ═══════════════════════════════════════════════════════════
    //  GET — Toutes les maintenances (zones du technicien)
    // ═══════════════════════════════════════════════════════════

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            Utilisateur technicien = getConnecte();
            List<Long> zoneIds = getZoneIds(technicien);
            if (zoneIds.isEmpty()) return ResponseEntity.ok(List.of());

            List<MaintenanceDto> result = new ArrayList<>();
            for (Long zid : zoneIds) {
                result.addAll(maintenanceService.getByZone(zid));
            }
            // Dédoublonner par id
            Map<Long, MaintenanceDto> map = new LinkedHashMap<>();
            result.forEach(m -> map.put(m.getId(), m));
            return ResponseEntity.ok(new ArrayList<>(map.values()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err(e));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET — Par ID
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            Utilisateur tech = getConnecte();
            MaintenanceDto dto = maintenanceService.getById(id);
            checkAcces(tech, dto);
            return ResponseEntity.ok(dto);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err(se));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err(e));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET — Par véhicule
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/vehicule/{matricule:.+}")
    public ResponseEntity<?> getByVehicule(@PathVariable String matricule) {
        try {
            Utilisateur tech = getConnecte();
            checkVehiculeAcces(tech, matricule);
            return ResponseEntity.ok(maintenanceService.getByVehicule(matricule));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err(se));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err(e));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET — Par statut / type
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/statut/{statut}")
    public ResponseEntity<?> getByStatut(@PathVariable StatutMaintenance statut) {
        try {
            Utilisateur tech = getConnecte();
            List<Long> zoneIds = getZoneIds(tech);
            List<MaintenanceDto> all = maintenanceService.getByStatut(statut);
            List<MaintenanceDto> filtered = all.stream()
                    .filter(m -> m.getVehiculeZoneNom() == null ||
                            getZoneNoms(tech).contains(m.getVehiculeZoneNom()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err(e));
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<?> getByType(@PathVariable TypeIntervention type) {
        try {
            Utilisateur tech = getConnecte();
            List<MaintenanceDto> all = maintenanceService.getByType(type);
            Set<String> zoneNoms = getZoneNoms(tech);
            List<MaintenanceDto> filtered = all.stream()
                    .filter(m -> m.getVehiculeZoneNom() == null ||
                            zoneNoms.contains(m.getVehiculeZoneNom()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err(e));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  POST — Créer un dossier
    // ═══════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> creer(@RequestBody MaintenanceRequest req) {
        try {
            Utilisateur tech = getConnecte();
            checkVehiculeAcces(tech, req.getVehiculeMatricule());
            MaintenanceDto dto = maintenanceService.creer(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SuccessResponse("Dossier créé avec succès", dto));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err(se));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err(e));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PUT — Modifier
    // ═══════════════════════════════════════════════════════════

    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@PathVariable Long id, @RequestBody MaintenanceRequest req) {
        try {
            Utilisateur tech = getConnecte();
            MaintenanceDto existing = maintenanceService.getById(id);
            checkAcces(tech, existing);
            MaintenanceDto dto = maintenanceService.modifier(id, req);
            return ResponseEntity.ok(new SuccessResponse("Dossier modifié", dto));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err(se));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err(e));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DELETE — Supprimer
    // ═══════════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        try {
            Utilisateur tech = getConnecte();
            MaintenanceDto existing = maintenanceService.getById(id);
            checkAcces(tech, existing);
            maintenanceService.supprimer(id);
            return ResponseEntity.ok(new MsgResponse("Dossier supprimé"));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err(se));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err(e));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Détails
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{id}/details")
    public ResponseEntity<?> ajouterDetail(@PathVariable Long id,
                                           @RequestBody DetailMaintenanceDto req) {
        try {
            Utilisateur tech = getConnecte();
            checkAcces(tech, maintenanceService.getById(id));
            return ResponseEntity.ok(new SuccessResponse("Détail ajouté",
                    maintenanceService.ajouterDetail(id, req)));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err(se));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err(e));
        }
    }

    @DeleteMapping("/{maintenanceId}/details/{detailId}")
    public ResponseEntity<?> supprimerDetail(@PathVariable Long maintenanceId,
                                             @PathVariable Long detailId) {
        try {
            Utilisateur tech = getConnecte();
            checkAcces(tech, maintenanceService.getById(maintenanceId));
            return ResponseEntity.ok(new SuccessResponse("Détail supprimé",
                    maintenanceService.supprimerDetail(maintenanceId, detailId)));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err(se));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(err(e));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Dashboard
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Utilisateur tech = getConnecte();
            List<Long> zoneIds = getZoneIds(tech);

            List<MaintenanceDto> all = new ArrayList<>();
            for (Long zid : zoneIds) {
                all.addAll(maintenanceService.getByZone(zid));
            }
            Map<Long, MaintenanceDto> map = new LinkedHashMap<>();
            all.forEach(m -> map.put(m.getId(), m));
            List<MaintenanceDto> maintenances = new ArrayList<>(map.values());

            Map<String, Object> dash = new LinkedHashMap<>();
            dash.put("nbDossiers", maintenances.size());
            dash.put("totalHtva",  round3(maintenances.stream().mapToDouble(MaintenanceDto::getCoutTotalHtva).sum()));
            dash.put("nbEnCours",  maintenances.stream().filter(m -> StatutMaintenance.EN_COURS.name().equals(m.getStatut().name())).count());
            dash.put("nbTermines", maintenances.stream().filter(m -> StatutMaintenance.TERMINEE.name().equals(m.getStatut().name())).count());
            dash.put("nbAnnules",  maintenances.stream().filter(m -> StatutMaintenance.ANNULEE.name().equals(m.getStatut().name())).count());

            // Par type
            Map<String, Long> parType = maintenances.stream()
                    .collect(Collectors.groupingBy(m -> m.getTypeIntervention().name(), Collectors.counting()));
            dash.put("parType", parType);

            // Par véhicule (top 5)
            Map<String, Double> parVehicule = new LinkedHashMap<>();
            maintenances.stream()
                    .collect(Collectors.groupingBy(MaintenanceDto::getVehiculeMatricule,
                            Collectors.summingDouble(MaintenanceDto::getCoutTotalHtva)))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(5)
                    .forEach(e -> parVehicule.put(e.getKey(), round3(e.getValue())));
            dash.put("topVehicules", parVehicule);

            // Derniers dossiers
            List<Map<String, Object>> derniers = maintenances.stream()
                    .sorted(Comparator.comparing(m -> m.getDateCreation() != null ? m.getDateCreation().toString() : "", Comparator.reverseOrder()))
                    .limit(5)
                    .map(m -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", m.getId());
                        row.put("numeroDossier", m.getNumeroDossier());
                        row.put("vehicule", m.getVehiculeMatricule());
                        row.put("type", m.getTypeIntervention().name());
                        row.put("statut", m.getStatut().name());
                        row.put("htva", m.getCoutTotalHtva());
                        row.put("date", m.getDateIntervention());
                        return row;
                    }).collect(Collectors.toList());
            dash.put("derniersDossiers", derniers);

            return ResponseEntity.ok(dash);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err(e));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPORT EXCEL — Toutes les maintenances des zones
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exporterExcelGlobal(
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String statut) {
        try {
            Utilisateur tech = getConnecte();
            List<Long> autorises = getZoneIds(tech);

            // Si zoneId fourni, vérifier qu'il fait partie des zones autorisées
            List<Long> targetZones = (zoneId != null && autorises.contains(zoneId))
                    ? List.of(zoneId) : autorises;

            List<MaintenanceDto> all = new ArrayList<>();
            for (Long zid : targetZones) {
                all.addAll(maintenanceService.getByZone(zid));
            }

            // Déduplication
            Map<Long, MaintenanceDto> map = new LinkedHashMap<>();
            all.forEach(m -> map.put(m.getId(), m));
            List<MaintenanceDto> maintenances = new ArrayList<>(map.values());

            // Filtre statut optionnel
            if (statut != null && !statut.isBlank()) {
                maintenances = maintenances.stream()
                        .filter(m -> m.getStatut().name().equals(statut))
                        .collect(Collectors.toList());
            }

            byte[] bytes = buildExcelGlobal(maintenances, tech.getNom());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"maintenances_zones.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPORT EXCEL — Un seul dossier avec ses détails
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/{id}/export/excel")
    public ResponseEntity<byte[]> exporterExcelDossier(@PathVariable Long id) {
        try {
            Utilisateur tech = getConnecte();
            MaintenanceDto dto = maintenanceService.getById(id);
            checkAcces(tech, dto);

            byte[] bytes = buildExcelDossier(dto);
            String filename = "maintenance_" + dto.getNumeroDossier()
                    .replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  BUILDER EXCEL — Global (toutes zones)
    // ═══════════════════════════════════════════════════════════

    private byte[] buildExcelGlobal(List<MaintenanceDto> maintenances, String techNom) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();

        // ── Feuille 1 : Résumé ─────────────────────────────────
        XSSFSheet ws1 = wb.createSheet("Résumé");
        ws1.setColumnWidth(0, 22 * 256);
        ws1.setColumnWidth(1, 18 * 256);
        ws1.setColumnWidth(2, 18 * 256);
        ws1.setColumnWidth(3, 20 * 256);
        ws1.setColumnWidth(4, 16 * 256);
        ws1.setColumnWidth(5, 14 * 256);
        ws1.setColumnWidth(6, 16 * 256);

        CellStyle stTitle  = mkStyle(wb, COLOR_TITLE_BG,  null, "FFFFFF", true,  14, HorizontalAlignment.CENTER);
        CellStyle stHeader = mkStyle(wb, COLOR_HEADER_BG, null, "FFFFFF", true,  10, HorizontalAlignment.CENTER);
        CellStyle stData   = mkStyle(wb, null, null, "000000", false, 10, HorizontalAlignment.LEFT);
        CellStyle stNum    = mkStyle(wb, null, "#,##0.000", "000000", false, 10, HorizontalAlignment.RIGHT);
        CellStyle stTotal  = mkStyle(wb, COLOR_TOTAL_BG, "#,##0.000", "000000", true, 10, HorizontalAlignment.RIGHT);
        CellStyle stInfo   = mkStyle(wb, COLOR_MO_BG, null, "000000", false, 10, HorizontalAlignment.LEFT);

        int row = 0;

        // Titre
        Row r0 = ws1.createRow(row++);
        r0.setHeightInPoints(28);
        Cell cT = r0.createCell(0);
        cT.setCellValue("GESTION MAINTENANCE — " + techNom.toUpperCase());
        cT.setCellStyle(stTitle);
        ws1.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        // Info technicien + date
        Row r1 = ws1.createRow(row++);
        Cell ci = r1.createCell(0);
        ci.setCellValue("Technicien : " + techNom + "   |   Exporté le : " + LocalDate.now());
        ci.setCellStyle(stInfo);
        ws1.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        row++;

        // En-têtes
        Row rh = ws1.createRow(row++);
        rh.setHeightInPoints(30);
        String[] hdrs = {"N° Dossier","Véhicule","Marque/Modèle","Zone","Type","Statut","Total HTVA (DT)"};
        for (int i = 0; i < hdrs.length; i++) {
            Cell c = rh.createCell(i);
            c.setCellValue(hdrs[i]);
            c.setCellStyle(stHeader);
        }

        // Données
        int dataStart = row + 1;
        for (MaintenanceDto m : maintenances) {
            Row r = ws1.createRow(row++);
            r.setHeightInPoints(18);
            CellStyle rowStyle = m.getStatut().name().equals("EN_COURS")
                    ? mkStyle(wb, COLOR_WARN_BG, null, "000000", false, 10, HorizontalAlignment.LEFT) : stData;

            setStr(r, 0, m.getNumeroDossier(), rowStyle);
            setStr(r, 1, m.getVehiculeMatricule(), rowStyle);
            setStr(r, 2, m.getVehiculeMarqueModele() != null ? m.getVehiculeMarqueModele() : "—", rowStyle);
            setStr(r, 3, m.getVehiculeZoneNom() != null ? m.getVehiculeZoneNom() : "—", rowStyle);
            setStr(r, 4, getTypeLabel(m.getTypeIntervention().name()), rowStyle);
            setStr(r, 5, getStatutLabel(m.getStatut().name()), rowStyle);
            setNum(r, 6, m.getCoutTotalHtva(), stNum);
        }

        // Total
        if (!maintenances.isEmpty()) {
            Row rTot = ws1.createRow(row++);
            rTot.setHeightInPoints(22);
            Cell cTotLbl = rTot.createCell(0);
            cTotLbl.setCellValue("TOTAL");
            cTotLbl.setCellStyle(stTotal);
            ws1.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 5));
            Cell cTotVal = rTot.createCell(6);
            cTotVal.setCellFormula("SUM(G" + dataStart + ":G" + (row - 1) + ")");
            cTotVal.setCellStyle(stTotal);
        }

        // ── Feuille 2 : Détails par véhicule ─────────────────
        // Grouper par véhicule
        Map<String, List<MaintenanceDto>> byVeh = maintenances.stream()
                .collect(Collectors.groupingBy(MaintenanceDto::getVehiculeMatricule));

        for (Map.Entry<String, List<MaintenanceDto>> entry : byVeh.entrySet()) {
            String mat = entry.getKey();
            String safeName = mat.replace(":", "").replace("/", "-");
            if (safeName.length() > 31) safeName = safeName.substring(0, 31);

            XSSFSheet ws2 = wb.createSheet(safeName);
            ws2.setColumnWidth(0, 14 * 256);
            ws2.setColumnWidth(1, 14 * 256);
            ws2.setColumnWidth(2, 22 * 256);
            ws2.setColumnWidth(3, 10 * 256);
            ws2.setColumnWidth(4, 14 * 256);
            ws2.setColumnWidth(5, 14 * 256);

            int r2 = 0;
            Row rtit = ws2.createRow(r2++);
            rtit.setHeightInPoints(22);
            Cell ct2 = rtit.createCell(0);
            ct2.setCellValue("Véhicule : " + mat);
            ct2.setCellStyle(stTitle);
            ws2.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            for (MaintenanceDto m : entry.getValue()) {
                r2++;
                Row rdoss = ws2.createRow(r2++);
                rdoss.setHeightInPoints(18);
                Cell cd = rdoss.createCell(0);
                cd.setCellValue("Dossier #" + m.getNumeroDossier() + "  —  "
                        + getTypeLabel(m.getTypeIntervention().name())
                        + "  —  " + getStatutLabel(m.getStatut().name())
                        + "  —  " + (m.getDateIntervention() != null ? m.getDateIntervention() : "—"));
                cd.setCellStyle(mkStyle(wb, COLOR_MO_BG, null, "000000", true, 10, HorizontalAlignment.LEFT));
                ws2.addMergedRegion(new CellRangeAddress(r2 - 1, r2 - 1, 0, 5));

                if (m.getDetails() != null && !m.getDetails().isEmpty()) {
                    // Sous-en-têtes
                    Row rhd = ws2.createRow(r2++);
                    String[] dHdrs = {"Type","Marque","Désignation","Qté","Montant HT","Total HTVA"};
                    for (int i = 0; i < dHdrs.length; i++) {
                        Cell c = rhd.createCell(i);
                        c.setCellValue(dHdrs[i]);
                        c.setCellStyle(stHeader);
                    }
                    for (DetailMaintenanceDto d : m.getDetails()) {
                        Row rd = ws2.createRow(r2++);
                        rd.setHeightInPoints(16);
                        CellStyle ds = d.getType() != null && d.getType().name().equals("MAIN_D_OEUVRE")
                                ? mkStyle(wb, COLOR_MO_BG, null, "000000", false, 10, HorizontalAlignment.LEFT)
                                : mkStyle(wb, COLOR_PIECE_BG, null, "000000", false, 10, HorizontalAlignment.LEFT);
                        setStr(rd, 0, d.getType() != null ? (d.getType().name().equals("MAIN_D_OEUVRE") ? "Main d'œuvre" : "Pièce") : "—", ds);
                        setStr(rd, 1, d.getMarque() != null ? d.getMarque() : "—", ds);
                        setStr(rd, 2, d.getDesignation(), ds);
                        setNum(rd, 3, d.getQuantite(), stNum);
                        setNum(rd, 4, d.getMontantUnitaire(), stNum);
                        setNum(rd, 5, d.getTotalHtva(), stNum);
                    }
                    // Sous-total
                    Row rsub = ws2.createRow(r2++);
                    rsub.setHeightInPoints(18);
                    Cell csub = rsub.createCell(0);
                    csub.setCellValue("Total HTVA dossier :");
                    csub.setCellStyle(stTotal);
                    ws2.addMergedRegion(new CellRangeAddress(r2 - 1, r2 - 1, 0, 4));
                    setNum(rsub, 5, m.getCoutTotalHtva(), stTotal);
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════
    //  BUILDER EXCEL — Dossier unique
    // ═══════════════════════════════════════════════════════════

    private byte[] buildExcelDossier(MaintenanceDto m) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet ws = wb.createSheet("Dossier " + m.getNumeroDossier());

        ws.setColumnWidth(0, 20 * 256);
        ws.setColumnWidth(1, 18 * 256);
        ws.setColumnWidth(2, 30 * 256);
        ws.setColumnWidth(3, 8  * 256);
        ws.setColumnWidth(4, 14 * 256);
        ws.setColumnWidth(5, 14 * 256);

        CellStyle stTitle  = mkStyle(wb, COLOR_TITLE_BG,  null, "FFFFFF", true, 14, HorizontalAlignment.CENTER);
        CellStyle stHeader = mkStyle(wb, COLOR_HEADER_BG, null, "FFFFFF", true, 10, HorizontalAlignment.CENTER);
        CellStyle stMeta   = mkStyle(wb, COLOR_MO_BG, null, "000000", false, 10, HorizontalAlignment.LEFT);
        CellStyle stMo     = mkStyle(wb, COLOR_MO_BG, null, "000000", false, 10, HorizontalAlignment.LEFT);
        CellStyle stPiece  = mkStyle(wb, COLOR_PIECE_BG, null, "000000", false, 10, HorizontalAlignment.LEFT);
        CellStyle stNum    = mkStyle(wb, null, "#,##0.000", "000000", false, 10, HorizontalAlignment.RIGHT);
        CellStyle stTotal  = mkStyle(wb, COLOR_TOTAL_BG, "#,##0.000", "000000", true, 10, HorizontalAlignment.RIGHT);
        CellStyle stSec    = mkStyle(wb, COLOR_HEADER_BG, null, "FFFFFF", true, 10, HorizontalAlignment.LEFT);

        int row = 0;

        // Titre
        Row r0 = ws.createRow(row++);
        r0.setHeightInPoints(28);
        Cell cT = r0.createCell(0);
        cT.setCellValue("DOSSIER DE MAINTENANCE N° " + m.getNumeroDossier());
        cT.setCellStyle(stTitle);
        ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        row++;

        // Métadonnées
        String[][] meta = {
                {"Véhicule", m.getVehiculeMatricule()},
                {"Marque / Modèle", m.getVehiculeMarqueModele() != null ? m.getVehiculeMarqueModele() : "—"},
                {"Zone", m.getVehiculeZoneNom() != null ? m.getVehiculeZoneNom() : "—"},
                {"Type d'intervention", getTypeLabel(m.getTypeIntervention().name())},
                {"Statut", getStatutLabel(m.getStatut().name())},
                {"Date d'intervention", m.getDateIntervention() != null ? m.getDateIntervention().toString() : "—"},
                {"Description", m.getDescription() != null ? m.getDescription() : "—"},
                {"Créé par", m.getCreePar() != null ? m.getCreePar() : "—"},
        };
        for (String[] pair : meta) {
            Row r = ws.createRow(row++);
            r.setHeightInPoints(18);
            Cell cl = r.createCell(0);
            cl.setCellValue(pair[0]);
            cl.setCellStyle(mkStyle(wb, COLOR_MO_BG, null, "000000", true, 10, HorizontalAlignment.LEFT));
            Cell cv = r.createCell(1);
            cv.setCellValue(pair[1]);
            cv.setCellStyle(stMeta);
            ws.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 1, 5));
        }

        if (m.getDetails() == null || m.getDetails().isEmpty()) {
            row++;
            Row rEmpty = ws.createRow(row++);
            Cell ce = rEmpty.createCell(0);
            ce.setCellValue("Aucun détail enregistré");
            ce.setCellStyle(stMeta);
            ws.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 5));
        } else {
            // ── Section Main d'œuvre ──
            List<DetailMaintenanceDto> mos = m.getDetails().stream()
                    .filter(d -> d.getType() != null && d.getType() == TypeDetailMaintenance.MAIN_D_OEUVRE)
                    .collect(Collectors.toList());

            if (!mos.isEmpty()) {
                row++;
                Row rsec1 = ws.createRow(row++);
                rsec1.setHeightInPoints(20);
                Cell csec1 = rsec1.createCell(0);
                csec1.setCellValue("🔧  MAIN D'ŒUVRE");
                csec1.setCellStyle(stSec);
                ws.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 5));

                Row rhd1 = ws.createRow(row++);
                String[] h1 = {"Marque","N° Prestation","Désignation","Qté","Montant HT","Total HTVA"};
                for (int i = 0; i < h1.length; i++) {
                    Cell c = rhd1.createCell(i);
                    c.setCellValue(h1[i]);
                    c.setCellStyle(stHeader);
                }

                double totMo = 0;
                for (DetailMaintenanceDto d : mos) {
                    Row rd = ws.createRow(row++);
                    rd.setHeightInPoints(16);
                    setStr(rd, 0, d.getMarque() != null ? d.getMarque() : "—", stMo);
                    setStr(rd, 1, d.getNumero() != null ? d.getNumero() : "—", stMo);
                    setStr(rd, 2, d.getDesignation(), stMo);
                    setNum(rd, 3, d.getQuantite(), stNum);
                    setNum(rd, 4, d.getMontantUnitaire(), stNum);
                    setNum(rd, 5, d.getTotalHtva(), stNum);
                    totMo += d.getTotalHtva();
                }
                Row rSubMo = ws.createRow(row++);
                rSubMo.setHeightInPoints(18);
                Cell cSubLbl = rSubMo.createCell(0);
                cSubLbl.setCellValue("Total Main d'œuvre :");
                cSubLbl.setCellStyle(stTotal);
                ws.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 4));
                setNum(rSubMo, 5, totMo, stTotal);
            }

            // ── Section Pièces ──
            List<DetailMaintenanceDto> pieces = m.getDetails().stream()
                    .filter(d -> d.getType() != null && d.getType() == TypeDetailMaintenance.PIECE)
                    .collect(Collectors.toList());

            if (!pieces.isEmpty()) {
                row++;
                Row rsec2 = ws.createRow(row++);
                rsec2.setHeightInPoints(20);
                Cell csec2 = rsec2.createCell(0);
                csec2.setCellValue("🔩  PIÈCES");
                csec2.setCellStyle(stSec);
                ws.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 5));

                Row rhd2 = ws.createRow(row++);
                String[] h2 = {"Marque","N° Pièce","Désignation","Qté","Montant HT","Total HTVA"};
                for (int i = 0; i < h2.length; i++) {
                    Cell c = rhd2.createCell(i);
                    c.setCellValue(h2[i]);
                    c.setCellStyle(stHeader);
                }

                double totPiece = 0;
                for (DetailMaintenanceDto d : pieces) {
                    Row rd = ws.createRow(row++);
                    rd.setHeightInPoints(16);
                    setStr(rd, 0, d.getMarque() != null ? d.getMarque() : "—", stPiece);
                    setStr(rd, 1, d.getNumeroPiece() != null ? d.getNumeroPiece() : "—", stPiece);
                    setStr(rd, 2, d.getDesignation(), stPiece);
                    setNum(rd, 3, d.getQuantite(), stNum);
                    setNum(rd, 4, d.getMontantUnitaire(), stNum);
                    setNum(rd, 5, d.getTotalHtva(), stNum);
                    totPiece += d.getTotalHtva();
                }
                Row rSubP = ws.createRow(row++);
                rSubP.setHeightInPoints(18);
                Cell cPLbl = rSubP.createCell(0);
                cPLbl.setCellValue("Total Pièces :");
                cPLbl.setCellStyle(stTotal);
                ws.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 4));
                setNum(rSubP, 5, totPiece, stTotal);
            }

            // ── Grand total ──
            row++;
            Row rGT = ws.createRow(row++);
            rGT.setHeightInPoints(22);
            Cell cGTLbl = rGT.createCell(0);
            cGTLbl.setCellValue("TOTAL HTVA DOSSIER :");
            CellStyle stGT = mkStyle(wb, COLOR_TITLE_BG, "#,##0.000", "FFFFFF", true, 12, HorizontalAlignment.RIGHT);
            cGTLbl.setCellStyle(stGT);
            ws.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 4));
            Cell cGTVal = rGT.createCell(5);
            cGTVal.setCellValue(m.getCoutTotalHtva());
            cGTVal.setCellStyle(stGT);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS EXCEL
    // ═══════════════════════════════════════════════════════════

    private CellStyle mkStyle(XSSFWorkbook wb, byte[] bgRgb, String numFmt,
                              String fgHex, boolean bold, int size,
                              HorizontalAlignment align) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(bold);
        f.setFontHeightInPoints((short) size);
        if (fgHex != null) {
            byte[] rgb = hexToBytes(fgHex);
            f.setColor(new XSSFColor(rgb, null));
        }
        s.setFont(f);
        if (bgRgb != null) {
            s.setFillForegroundColor(new XSSFColor(bgRgb, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (numFmt != null) s.setDataFormat(wb.createDataFormat().getFormat(numFmt));
        s.setAlignment(align);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderTop(BorderStyle.THIN); s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private void setStr(Row r, int col, String val, CellStyle s) {
        Cell c = r.createCell(col); c.setCellValue(val != null ? val : "—"); c.setCellStyle(s);
    }
    private void setNum(Row r, int col, double val, CellStyle s) {
        Cell c = r.createCell(col); c.setCellValue(val); c.setCellStyle(s);
    }
    private byte[] hexToBytes(String hex) {
        hex = hex.replace("#", "");
        return new byte[]{
                (byte) Integer.parseInt(hex.substring(0, 2), 16),
                (byte) Integer.parseInt(hex.substring(2, 4), 16),
                (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    // ═══════════════════════════════════════════════════════════
    //  SÉCURITÉ
    // ═══════════════════════════════════════════════════════════

    private Utilisateur getConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private List<Long> getZoneIds(Utilisateur u) {
        return affectationRepository.findByUtilisateur(u).stream()
                .map(a -> a.getZone().getId())
                .collect(Collectors.toList());
    }

    private Set<String> getZoneNoms(Utilisateur u) {
        return affectationRepository.findByUtilisateur(u).stream()
                .map(a -> a.getZone().getNom())
                .collect(Collectors.toSet());
    }

    private void checkAcces(Utilisateur tech, MaintenanceDto dto) {
        Set<String> zoneNoms = getZoneNoms(tech);
        if (dto.getVehiculeZoneNom() != null && !zoneNoms.contains(dto.getVehiculeZoneNom())) {
            throw new SecurityException("Accès refusé à ce dossier de maintenance.");
        }
    }

    private void checkVehiculeAcces(Utilisateur tech, String matricule) {
        Vehicule v = vehiculeRepository.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));
        if (v.getZone() == null) return; // Pas de zone = pas de restriction zone
        List<Long> zoneIds = getZoneIds(tech);
        if (!zoneIds.contains(v.getZone().getId())) {
            throw new SecurityException("Accès refusé : véhicule hors de vos zones.");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  LABELS
    // ═══════════════════════════════════════════════════════════

    private String getTypeLabel(String type) {
        Map<String, String> m = Map.of(
                "PREVENTIVE", "Préventive", "CORRECTIVE", "Corrective",
                "VISITE_TECHNIQUE", "Visite technique", "ACCIDENT", "Accident");
        return m.getOrDefault(type, type);
    }

    private String getStatutLabel(String statut) {
        Map<String, String> m = Map.of(
                "EN_COURS", "En cours", "TERMINEE", "Terminée", "ANNULEE", "Annulée");
        return m.getOrDefault(statut, statut);
    }

    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    // ═══════════════════════════════════════════════════════════
    //  Records
    // ═══════════════════════════════════════════════════════════
    private record ErrResponse(String message) {}
    private record MsgResponse(String message) {}
    private record SuccessResponse(String message, MaintenanceDto data) {}
    private ErrResponse err(Exception e) { return new ErrResponse(e.getMessage()); }
}