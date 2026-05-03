package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.CarburantVehiculeDto;
import com.example.ttcarburant.dto.CarburantVehiculeRequest;
import com.example.ttcarburant.model.entity.GestionCarburantVehicule;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.repository.CarburantVehiculeRepository;
import com.example.ttcarburant.repository.UtilisateurRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import com.example.ttcarburant.services.CarburantVehiculeService;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Technicien/Conducteur — Gestion complète du carburant (CRUD + Export Excel).
 *
 * Routes :
 *   GET    /api/technicien/carburant/mes-vehicules
 *   GET    /api/technicien/carburant/historique/{matricule}
 *   GET    /api/technicien/carburant/prefill/{matricule}
 *   POST   /api/technicien/carburant/saisir
 *   PUT    /api/technicien/carburant/modifier/{id}
 *   DELETE /api/technicien/carburant/supprimer/{id}
 *   GET    /api/technicien/carburant/stats/{matricule}
 *   GET    /api/technicien/carburant/dashboard
 *   GET    /api/technicien/carburant/export/excel/{matricule}
 *   GET    /api/technicien/carburant/export/excel/periode
 */
@RestController
@RequestMapping("/api/technicien/carburant")
@PreAuthorize("hasRole('TECHNICIEN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class TechnicienCarburantController {

    private static final String[] MOIS_LABELS = {
            "", "Janvier","Février","Mars","Avril","Mai","Juin",
            "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
    };

    // ── Couleurs Excel (identiques au fichier carburant_2026_05) ──
    private static final byte[] COLOR_TITLE_BG  = {(byte)0x0C,(byte)0x37,(byte)0x84}; // #0C3784
    private static final byte[] COLOR_ZONE_BG   = {(byte)0xD2,(byte)0xE1,(byte)0xFF}; // #D2E1FF
    private static final byte[] COLOR_HEADER_BG = {(byte)0x1F,(byte)0x49,(byte)0x7D}; // #1F497D
    private static final byte[] COLOR_CALC_BG   = {(byte)0xEB,(byte)0xFF,(byte)0xEB}; // #EBFFEB
    private static final byte[] COLOR_CALC_FG   = {(byte)0x00,(byte)0x61,(byte)0x00}; // #006100
    private static final byte[] COLOR_TOTAL_BG  = {(byte)0xC6,(byte)0xE0,(byte)0xB4}; // #C6E0B4
    private static final byte[] COLOR_NOTE_BG   = {(byte)0xF2,(byte)0xF2,(byte)0xF2}; // #F2F2F2
    private static final byte[] COLOR_NOTE_FG   = {(byte)0x59,(byte)0x59,(byte)0x59}; // #595959
    private static final byte[] COLOR_ALERT_BG  = {(byte)0xFF,(byte)0xEB,(byte)0xEB}; // #FFEBEB
    private static final byte[] COLOR_ALERT_FG  = {(byte)0x9C,(byte)0x00,(byte)0x06}; // #9C0006

    private final UtilisateurRepository utilisateurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final CarburantVehiculeRepository carburantRepository;
    private final CarburantVehiculeService carburantVehiculeService;

    public TechnicienCarburantController(
            UtilisateurRepository utilisateurRepository,
            VehiculeRepository vehiculeRepository,
            CarburantVehiculeRepository carburantRepository,
            CarburantVehiculeService carburantVehiculeService) {
        this.utilisateurRepository    = utilisateurRepository;
        this.vehiculeRepository       = vehiculeRepository;
        this.carburantRepository      = carburantRepository;
        this.carburantVehiculeService = carburantVehiculeService;
    }

    // ══════════════════════════════════════════════════════════════
    //  GET — mes véhicules (avec données carburant mois courant)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/mes-vehicules")
    public ResponseEntity<?> getMesVehicules() {
        try {
            Utilisateur u = getConnecte();
            List<Vehicule> vehicules = findVehiculesParUtilisateur(u);

            int moisCourant   = LocalDate.now().getMonthValue();
            int anneeCourante = LocalDate.now().getYear();

            List<Map<String, Object>> result = vehicules.stream().map(v -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("matricule",        v.getMatricule());
                item.put("marqueModele",     v.getMarqueModele());
                item.put("typeVehicule",     v.getTypeVehicule());
                item.put("typeCarburant",    v.getTypeCarburant());
                item.put("prixCarburant",    v.getPrixCarburant());
                item.put("coutDuMois",       v.getCoutDuMois());
                item.put("kilometrageTotal", v.getKilometrageTotal());
                item.put("nomConducteur",    v.getNomConducteur());
                item.put("prenomConducteur", v.getPrenomConducteur());
                if (v.getZone() != null) {
                    item.put("zoneId",  v.getZone().getId());
                    item.put("zoneNom", v.getZone().getNom());
                }
                Optional<GestionCarburantVehicule> moisOpt =
                        carburantRepository.findByVehiculeAndAnneeAndMois(v, anneeCourante, moisCourant);
                item.put("saisieExistante", moisOpt.isPresent());
                moisOpt.ifPresent(g -> {
                    item.put("distanceMois",      g.getDistanceParcourue());
                    item.put("consoMoisLitres",   Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir()));
                    item.put("budgetDepasse",     g.isBudgetDepasse());
                    item.put("depassementMontant",g.getDepassementMontant());
                    item.put("gestionId",         g.getId());
                });
                return item;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GET — historique carburant
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/historique/{matricule:.+}")
    public ResponseEntity<?> getHistorique(
            @PathVariable String matricule,
            @RequestParam(required = false) Integer annee) {
        try {
            Utilisateur u = getConnecte();
            Vehicule v    = findVehiculeAutorise(u, matricule);

            List<GestionCarburantVehicule> data = (annee != null)
                    ? carburantRepository.findByVehiculeAndAnneeOrderByMois(v, annee)
                    : carburantRepository.findByVehiculeOrderByAnneeDescMoisDesc(v);

            return ResponseEntity.ok(data.stream().map(this::toHistoriqueMap).collect(Collectors.toList()));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GET — pré-remplissage mois précédent
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/prefill/{matricule:.+}")
    public ResponseEntity<?> getPrefill(
            @PathVariable String matricule,
            @RequestParam int annee,
            @RequestParam int mois) {
        try {
            Utilisateur u = getConnecte();
            findVehiculeAutorise(u, matricule);
            return ResponseEntity.ok(carburantVehiculeService.getPrefillFromPreviousMonth(matricule, annee, mois));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  POST — saisir un ravitaillement
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/saisir")
    public ResponseEntity<?> saisir(@Valid @RequestBody CarburantVehiculeRequest req) {
        try {
            Utilisateur u = getConnecte();
            findVehiculeAutorise(u, req.getVehiculeMatricule());

            CarburantVehiculeDto dto = carburantVehiculeService.saisir(req);

            if (dto.isBudgetDepasse()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                        "message", "Ravitaillement enregistré ⚠️ Budget dépassé de " + dto.getDepassementMontant() + " DT",
                        "data", dto,
                        "alert", true
                ));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of("message", "Ravitaillement enregistré avec succès", "data", dto));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PUT — modifier une saisie
    // ══════════════════════════════════════════════════════════════

    @PutMapping("/modifier/{id}")
    public ResponseEntity<?> modifier(
            @PathVariable Long id,
            @Valid @RequestBody CarburantVehiculeRequest req) {
        try {
            Utilisateur u = getConnecte();
            findVehiculeAutorise(u, req.getVehiculeMatricule());

            CarburantVehiculeDto dto = carburantVehiculeService.modifier(id, req);

            if (dto.isBudgetDepasse()) {
                return ResponseEntity.ok(Map.of(
                        "message", "Saisie modifiée ⚠️ Budget dépassé de " + dto.getDepassementMontant() + " DT",
                        "data", dto,
                        "alert", true
                ));
            }
            return ResponseEntity.ok(Map.of("message", "Saisie modifiée avec succès", "data", dto));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DELETE — supprimer une saisie
    // ══════════════════════════════════════════════════════════════

    @DeleteMapping("/supprimer/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        try {
            Utilisateur u = getConnecte();

            // Vérifier que la saisie appartient à un véhicule autorisé
            GestionCarburantVehicule g = carburantRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Saisie introuvable"));
            findVehiculeAutorise(u, g.getVehicule().getMatricule());

            carburantVehiculeService.supprimer(id);
            return ResponseEntity.ok(Map.of("message", "Saisie supprimée avec succès"));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GET — stats / analytics par véhicule
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/stats/{matricule:.+}")
    public ResponseEntity<?> getStats(
            @PathVariable String matricule,
            @RequestParam(defaultValue = "0") int annee) {
        try {
            Utilisateur u = getConnecte();
            Vehicule v    = findVehiculeAutorise(u, matricule);

            int targetAnnee = annee > 0 ? annee : LocalDate.now().getYear();
            List<GestionCarburantVehicule> data =
                    carburantRepository.findByVehiculeAndAnneeOrderByMois(v, targetAnnee);

            return ResponseEntity.ok(buildStats(v, data, targetAnnee));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GET — dashboard global technicien
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Utilisateur u       = getConnecte();
            List<Vehicule> veh  = findVehiculesParUtilisateur(u);
            int annee           = LocalDate.now().getYear();
            int mois            = LocalDate.now().getMonthValue();

            Map<String, Object> dashboard = new LinkedHashMap<>();
            dashboard.put("totalVehicules", veh.size());

            double totalKm = 0, totalLitres = 0, totalDT = 0;
            int nbBudgetDepasses = 0;
            List<Map<String, Object>> vehiculesResume = new ArrayList<>();

            for (Vehicule v : veh) {
                List<GestionCarburantVehicule> anneeData =
                        carburantRepository.findByVehiculeAndAnneeOrderByMois(v, annee);

                double vKm  = anneeData.stream().mapToDouble(GestionCarburantVehicule::getDistanceParcourue).sum();
                double vL   = anneeData.stream().mapToDouble(g -> Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir())).sum();
                double vDT  = vL * v.getPrixCarburant();
                boolean hasBudgetDepasse = anneeData.stream().anyMatch(GestionCarburantVehicule::isBudgetDepasse);

                totalKm     += vKm;
                totalLitres += vL;
                totalDT     += vDT;
                if (hasBudgetDepasse) nbBudgetDepasses++;

                Optional<GestionCarburantVehicule> moisCourant =
                        carburantRepository.findByVehiculeAndAnneeAndMois(v, annee, mois);

                Map<String, Object> vMap = new LinkedHashMap<>();
                vMap.put("matricule",       v.getMatricule());
                vMap.put("marqueModele",    v.getMarqueModele());
                vMap.put("kmAnnee",         round3(vKm));
                vMap.put("litresAnnee",     round3(vL));
                vMap.put("coutAnnee",       round3(vDT));
                vMap.put("budgetDepasse",   hasBudgetDepasse);
                vMap.put("saisiesMois",     anneeData.size());
                vMap.put("moisCourantSaisi",moisCourant.isPresent());
                moisCourant.ifPresent(g -> {
                    vMap.put("kmMois",           g.getDistanceParcourue());
                    vMap.put("budgetMoisDepasse", g.isBudgetDepasse());
                });
                vehiculesResume.add(vMap);
            }

            dashboard.put("totalKmAnnee",      round3(totalKm));
            dashboard.put("totalLitresAnnee",  round3(totalLitres));
            dashboard.put("totalCoutAnnee",    round3(totalDT));
            dashboard.put("nbBudgetsDepasses", nbBudgetDepasses);
            dashboard.put("annee",             annee);
            dashboard.put("vehicules",         vehiculesResume);

            // Evolution mensuelle globale
            List<Map<String, Object>> evolution = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                final int fm = m;
                double mKm = 0, mL = 0;
                for (Vehicule v : veh) {
                    Optional<GestionCarburantVehicule> gOpt =
                            carburantRepository.findByVehiculeAndAnneeAndMois(v, annee, fm);
                    if (gOpt.isPresent()) {
                        mKm += gOpt.get().getDistanceParcourue();
                        mL  += Math.max(0, gOpt.get().getTotalRavitaillementLitres() - gOpt.get().getQuantiteRestanteReservoir());
                    }
                }
                Map<String, Object> mMap = new LinkedHashMap<>();
                mMap.put("mois",   m);
                mMap.put("label",  MOIS_LABELS[m].substring(0, 3));
                mMap.put("km",     round3(mKm));
                mMap.put("litres", round3(mL));
                evolution.add(mMap);
            }
            dashboard.put("evolutionMensuelle", evolution);

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  GET — Export Excel mensuel pour un véhicule (format DAF 2026)
    //        Couleurs identiques au fichier carburant_2026_05
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/export/excel/{matricule:.+}")
    public ResponseEntity<byte[]> exportExcelVehicule(
            @PathVariable String matricule,
            @RequestParam int annee,
            @RequestParam(required = false) Integer mois) {
        try {
            Utilisateur u = getConnecte();
            Vehicule v    = findVehiculeAutorise(u, matricule);

            List<GestionCarburantVehicule> data;
            String sheetLabel;
            if (mois != null) {
                data       = carburantRepository.findByVehiculeAndAnneeOrderByMois(v, annee)
                        .stream().filter(g -> g.getMois() == mois).collect(Collectors.toList());
                sheetLabel = MOIS_LABELS[mois] + " " + annee;
            } else {
                data       = carburantRepository.findByVehiculeAndAnneeOrderByMois(v, annee);
                sheetLabel = "Annuel " + annee;
            }

            byte[] bytes = buildExcel(data, v, sheetLabel);
            String filename = "carburant_" + v.getMatricule() + "_" + annee
                    + (mois != null ? "_" + String.format("%02d", mois) : "") + ".xlsx";

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

    // ══════════════════════════════════════════════════════════════
    //  GET — Export Excel période (tous les véhicules, un mois)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/export/excel/periode")
    public ResponseEntity<byte[]> exportExcelPeriode(
            @RequestParam int annee,
            @RequestParam int mois) {
        try {
            Utilisateur u       = getConnecte();
            List<Vehicule> veh  = findVehiculesParUtilisateur(u);

            // Récupérer toutes les saisies pour les véhicules autorisés
            List<GestionCarburantVehicule> data = new ArrayList<>();
            for (Vehicule v : veh) {
                carburantRepository.findByVehiculeAndAnneeAndMois(v, annee, mois).ifPresent(data::add);
            }

            String sheetLabel = MOIS_LABELS[mois] + " " + annee;
            byte[] bytes = buildExcel(data, null, sheetLabel);
            String filename = "carburant_" + annee + "_" + String.format("%02d", mois) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  EXCEL BUILDER — reproduit exactement le format DAF 2026
    //  (couleurs, colonnes, formules, notes)
    // ══════════════════════════════════════════════════════════════

    private byte[] buildExcel(List<GestionCarburantVehicule> data, Vehicule vehiculeUnique, String sheetLabel) throws Exception {

        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet    ws = wb.createSheet(sheetLabel);

        // Largeurs colonnes (en unités de 256e de caractère)
        int[] widths = {18, 22, 16, 12, 16, 16, 14, 14, 14, 12, 16, 14, 12};
        for (int i = 0; i < widths.length; i++)
            ws.setColumnWidth(i, widths[i] * 256);

        // ── Styles ───────────────────────────────────────────────

        CellStyle stTitle  = mkStyle(wb, COLOR_TITLE_BG,  null,         "FFFFFF", true,  14, HorizontalAlignment.CENTER);
        CellStyle stZone   = mkStyle(wb, COLOR_ZONE_BG,   null,         "000000", true,  11, HorizontalAlignment.LEFT);
        CellStyle stHeader = mkStyle(wb, COLOR_HEADER_BG, null,         "FFFFFF", true,  10, HorizontalAlignment.CENTER);
        CellStyle stData   = mkStyle(wb, null,            null,         "000000", false, 10, HorizontalAlignment.LEFT);
        CellStyle stNum    = mkStyle(wb, null,            "#,##0.000",  "000000", false, 10, HorizontalAlignment.RIGHT);
        CellStyle stCalc   = mkStyle(wb, COLOR_CALC_BG,  "#,##0.000",  null,     false, 10, HorizontalAlignment.RIGHT);
        CellStyle stAlert  = mkStyle(wb, COLOR_ALERT_BG, null,         null,     true,  10, HorizontalAlignment.CENTER);
        CellStyle stTotal  = mkStyle(wb, COLOR_TOTAL_BG, "#,##0.000",  "000000", true,  10, HorizontalAlignment.RIGHT);
        CellStyle stNote   = mkStyle(wb, COLOR_NOTE_BG,  null,         null,     false,  9, HorizontalAlignment.LEFT);

        // Couleur texte calc (vert foncé) et alert (rouge foncé)
        setFontColor(wb, stCalc,  COLOR_CALC_FG);
        setFontColor(wb, stAlert, COLOR_ALERT_FG);
        setFontColor(wb, stNote,  COLOR_NOTE_FG);

        stHeader.setWrapText(true);
        stZone.setIndention((short)1);
        stNote.setIndention((short)1);

        int row = 0;

        // ── Ligne 1 : Titre ──────────────────────────────────────
        Row r1 = ws.createRow(row++);
        r1.setHeightInPoints(28);
        Cell cTitle = r1.createCell(0);
        cTitle.setCellValue("GESTION CARBURANT VÉHICULES — " + sheetLabel.toUpperCase());
        cTitle.setCellStyle(stTitle);
        ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

        // ── Ligne 2 : Zone (si disponible) ───────────────────────
        String zoneNom = null;
        if (vehiculeUnique != null && vehiculeUnique.getZone() != null) {
            zoneNom = vehiculeUnique.getZone().getNom();
        } else if (!data.isEmpty() && data.get(0).getVehicule().getZone() != null) {
            zoneNom = data.get(0).getVehicule().getZone().getNom();
        }
        if (zoneNom != null) {
            Row r2 = ws.createRow(row++);
            Cell cZone = r2.createCell(0);
            cZone.setCellValue("Zone : " + zoneNom);
            cZone.setCellStyle(stZone);
            ws.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 12));
        }

        row++; // ligne vide

        // ── En-têtes colonnes ─────────────────────────────────────
        Row rHdr = ws.createRow(row++);
        rHdr.setHeightInPoints(36);
        String[] headers = {
                "Matricule","Marque / Modèle","Type Carb.","Prix (DT/L)",
                "Index Démarrage","Index Fin Mois","Distance (km)",
                "Total Ravit. (L)","Qté Restante (L)","% Conso",
                "Carb. Demandé (DT)","Budget (DT)","Alerte"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell c = rHdr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(stHeader);
        }

        // ── Lignes de données ─────────────────────────────────────
        int dataStart = row + 1; // row Excel (1-based)
        for (GestionCarburantVehicule g : data) {
            Row r = ws.createRow(row++);
            r.setHeightInPoints(18);
            Vehicule v = g.getVehicule();

            setStr(r, 0, v.getMatricule(),            stData);
            setStr(r, 1, v.getMarqueModele(),         stData);
            setStr(r, 2, v.getTypeCarburant().name(), stData);
            setNum(r, 3, v.getPrixCarburant(),        stNum);
            setNum(r, 4, g.getIndexDemarrageMois(),   stNum);
            setNum(r, 5, g.getIndexFinMois(),         stNum);
            setNum(r, 6, g.getDistanceParcourue(),    stCalc);
            setNum(r, 7, g.getTotalRavitaillementLitres(),    stCalc);
            setNum(r, 8, g.getQuantiteRestanteReservoir(),    stCalc);
            setNum(r, 9, g.getPourcentageConsommation(),      stCalc);
            setNum(r,10, g.getCarburantDemandeDinars(),       stCalc);
            setNum(r,11, v.getCoutDuMois(),           stNum);

            Cell cAlert = r.createCell(12);
            if (g.isBudgetDepasse()) {
                cAlert.setCellValue("⚠ +" + String.format("%.3f", g.getDepassementMontant()) + " DT");
                cAlert.setCellStyle(stAlert);
            } else {
                cAlert.setCellValue("✓ OK");
                cAlert.setCellStyle(stData);
            }
        }

        // ── Ligne TOTAUX ──────────────────────────────────────────
        if (!data.isEmpty()) {
            int lastDataRow = row; // Excel 1-based
            Row rTot = ws.createRow(row++);
            rTot.setHeightInPoints(20);

            Cell cTotLbl = rTot.createCell(0);
            cTotLbl.setCellValue("TOTAUX");
            cTotLbl.setCellStyle(stTotal);
            ws.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 5));

            // G = Distance, H = Total L, K = Carb. Demandé
            Cell cG = rTot.createCell(6);
            cG.setCellFormula("SUM(G" + dataStart + ":G" + lastDataRow + ")");
            cG.setCellStyle(stTotal);

            Cell cH = rTot.createCell(7);
            cH.setCellFormula("SUM(H" + dataStart + ":H" + lastDataRow + ")");
            cH.setCellStyle(stTotal);

            Cell cK = rTot.createCell(10);
            cK.setCellFormula("SUM(K" + dataStart + ":K" + lastDataRow + ")");
            cK.setCellStyle(stTotal);
        }

        // ── Ligne vide + Note formules DAF ────────────────────────
        row++;
        Row rNote = ws.createRow(row);
        Cell cNote = rNote.createCell(0);
        cNote.setCellValue(
                "Formules DAF 2026 — (1) Total L = (Ravit. préc. + Restant préc.) / Prix  " +
                        "(2) Qté rest. = Restant préc. / Prix  " +
                        "(3) Dist. = Index fin − Index démarrage  " +
                        "(4) % = (Total L − Qté rest.) / Distance  " +
                        "(5) Carb. DT = Budget − Restant préc."
        );
        cNote.setCellStyle(stNote);
        ws.addMergedRegion(new CellRangeAddress(row, row, 0, 12));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS STYLE EXCEL
    // ══════════════════════════════════════════════════════════════

    private CellStyle mkStyle(XSSFWorkbook wb, byte[] bgRgb, String numFmt,
                              String fgHex, boolean bold, int size,
                              HorizontalAlignment align) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(bold);
        f.setFontHeightInPoints((short) size);
        if (fgHex != null) f.setColor(new XSSFColor(hexToBytes(fgHex), null));
        s.setFont(f);
        if (bgRgb != null) {
            s.setFillForegroundColor(new XSSFColor(bgRgb, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (numFmt != null) s.setDataFormat(wb.createDataFormat().getFormat(numFmt));
        s.setAlignment(align);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN);
        return s;
    }

    private void setFontColor(XSSFWorkbook wb, CellStyle style, byte[] rgb) {
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(rgb, null));
        f.setFontHeightInPoints((short) 10);
        style.setFont(f);
    }

    private void setBorder(CellStyle s, BorderStyle bs) {
        s.setBorderTop(bs); s.setBorderBottom(bs);
        s.setBorderLeft(bs); s.setBorderRight(bs);
    }

    private void setStr(Row r, int col, String val, CellStyle s) {
        Cell c = r.createCell(col); c.setCellValue(val); c.setCellStyle(s);
    }

    private void setNum(Row r, int col, double val, CellStyle s) {
        Cell c = r.createCell(col); c.setCellValue(val); c.setCellStyle(s);
    }

    private byte[] hexToBytes(String hex) {
        hex = hex.replace("#","");
        return new byte[]{
                (byte) Integer.parseInt(hex.substring(0,2),16),
                (byte) Integer.parseInt(hex.substring(2,4),16),
                (byte) Integer.parseInt(hex.substring(4,6),16)
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS MÉTIER
    // ══════════════════════════════════════════════════════════════

    private Map<String, Object> toHistoriqueMap(GestionCarburantVehicule g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                          g.getId());
        m.put("annee",                       g.getAnnee());
        m.put("mois",                        g.getMois());
        m.put("periodeLabel",                MOIS_LABELS[g.getMois()] + " " + g.getAnnee());
        m.put("indexDemarrageMois",          g.getIndexDemarrageMois());
        m.put("indexFinMois",                g.getIndexFinMois());
        m.put("montantRestantMoisPrecedent", g.getMontantRestantMoisPrecedent());
        m.put("ravitaillementMoisPrecedent", g.getRavitaillementMoisPrecedent());
        m.put("ravitaillementMois",          g.getRavitaillementMois());
        m.put("totalRavitaillementLitres",   g.getTotalRavitaillementLitres());
        m.put("quantiteRestanteReservoir",   g.getQuantiteRestanteReservoir());
        m.put("distanceParcourue",           g.getDistanceParcourue());
        m.put("pourcentageConsommation",     g.getPourcentageConsommation());
        m.put("carburantDemandeDinars",      g.getCarburantDemandeDinars());
        m.put("montantRestantReservoirFin",  g.getMontantRestantReservoirFin());
        m.put("prixCarburant",               g.getVehicule().getPrixCarburant());
        m.put("coutDuMois",                  g.getVehicule().getCoutDuMois());
        m.put("budgetDepasse",               g.isBudgetDepasse());
        m.put("depassementMontant",          g.getDepassementMontant());
        m.put("dateCreation",                g.getDateCreation());

        double consoL   = Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
        double coutReel = consoL * g.getVehicule().getPrixCarburant();
        double tauxBudget = g.getVehicule().getCoutDuMois() > 0
                ? round2(coutReel / g.getVehicule().getCoutDuMois() * 100) : 0;

        m.put("tauxBudget",  tauxBudget);
        m.put("consoLitres", round3(consoL));
        m.put("coutReel",    round3(coutReel));
        return m;
    }

    private Map<String, Object> buildStats(Vehicule v, List<GestionCarburantVehicule> data, int annee) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("matricule",    v.getMatricule());
        stats.put("marqueModele", v.getMarqueModele());
        stats.put("annee",        annee);
        stats.put("nbMoisSaisis", data.size());

        double totalKm   = data.stream().mapToDouble(GestionCarburantVehicule::getDistanceParcourue).sum();
        double totalL    = data.stream().mapToDouble(g -> Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir())).sum();
        double totalCout = totalL * v.getPrixCarburant();
        double budget    = v.getCoutDuMois() * data.size();

        stats.put("totalKm",     round3(totalKm));
        stats.put("totalLitres", round3(totalL));
        stats.put("totalCout",   round3(totalCout));
        stats.put("totalBudget", round3(budget));
        stats.put("tauxBudget",  budget > 0 ? round2(totalCout / budget * 100) : 0);
        stats.put("rendementMoyen", totalKm > 0 ? round3(totalL / totalKm * 100) : 0);
        stats.put("nbBudgetsDepasses", data.stream().filter(GestionCarburantVehicule::isBudgetDepasse).count());

        List<Map<String, Object>> evolution = new ArrayList<>();
        for (int mo = 1; mo <= 12; mo++) {
            final int fm = mo;
            Map<String, Object> mMap = new LinkedHashMap<>();
            mMap.put("mois",  mo);
            mMap.put("label", MOIS_LABELS[mo].substring(0, 3));
            Optional<GestionCarburantVehicule> gOpt = data.stream().filter(g -> g.getMois() == fm).findFirst();
            if (gOpt.isPresent()) {
                GestionCarburantVehicule g  = gOpt.get();
                double cL = Math.max(0, g.getTotalRavitaillementLitres() - g.getQuantiteRestanteReservoir());
                mMap.put("km",            g.getDistanceParcourue());
                mMap.put("litres",        round3(cL));
                mMap.put("cout",          round3(cL * v.getPrixCarburant()));
                mMap.put("rendement",     g.getDistanceParcourue() > 0 ? round3(cL / g.getDistanceParcourue() * 100) : 0);
                mMap.put("budgetDepasse", g.isBudgetDepasse());
                mMap.put("budget",        v.getCoutDuMois());
                mMap.put("id",            g.getId()); // pour modification / suppression
            } else {
                mMap.put("km", 0); mMap.put("litres", 0); mMap.put("cout", 0);
                mMap.put("rendement", 0); mMap.put("budgetDepasse", false);
                mMap.put("budget", v.getCoutDuMois()); mMap.put("id", null);
            }
            evolution.add(mMap);
        }
        stats.put("evolution", evolution);
        return stats;
    }

    // ══════════════════════════════════════════════════════════════
    //  SÉCURITÉ — Accès aux véhicules
    // ══════════════════════════════════════════════════════════════

    private Utilisateur getConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private Vehicule findVehiculeAutorise(Utilisateur u, String matricule) {
        Vehicule v = vehiculeRepository.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));
        boolean autorise = findVehiculesParUtilisateur(u).stream()
                .anyMatch(av -> av.getMatricule().equals(matricule));
        if (!autorise) throw new SecurityException("Accès refusé : vous n'avez pas accès à ce véhicule.");
        return v;
    }

    private List<Vehicule> findVehiculesParUtilisateur(Utilisateur u) {
        if ("Conducteur".equalsIgnoreCase(u.getSpecialite())) {
            return vehiculeRepository.findAll().stream()
                    .filter(v -> isConducteurDuVehicule(u, v))
                    .collect(Collectors.toList());
        }
        return vehiculeRepository.findAll();
    }

    private boolean isConducteurDuVehicule(Utilisateur u, Vehicule v) {
        String nomV    = norm(v.getNomConducteur());
        String prenomV = norm(v.getPrenomConducteur());
        if (nomV.isEmpty() && prenomV.isEmpty()) return false;

        String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
        String local = email.contains("@") ? email.split("@")[0] : email;
        local = local.replaceAll("\\d+$", "");
        String prenomE = "", nomE = "";
        if (local.contains(".")) {
            String[] parts = local.split("\\.", 2);
            prenomE = norm(parts[0]); nomE = norm(parts[1]);
        } else { prenomE = norm(local); }

        boolean matchStrict  = !nomE.isEmpty() && !prenomE.isEmpty() && nomV.equals(nomE) && prenomV.equals(prenomE);
        boolean matchPartiel = (!nomE.isEmpty() && nomV.equals(nomE)) || (!prenomE.isEmpty() && prenomV.equals(prenomE));
        return matchStrict || matchPartiel;
    }

    private String norm(String s) {
        if (s == null || s.isEmpty()) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
    private double round2(double v) { return Math.round(v * 100.0)  / 100.0; }

    private record ErrorResponse(String message) {}
}