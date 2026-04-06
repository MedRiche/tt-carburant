package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.Maintenance.MaintenanceDto;
import com.example.ttcarburant.dto.Maintenance.MaintenanceRequest;
import com.example.ttcarburant.dto.Maintenance.DetailMaintenanceDto;
import com.example.ttcarburant.model.entity.DetailMaintenance;
import com.example.ttcarburant.model.entity.Maintenance;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.enums.StatutMaintenance;
import com.example.ttcarburant.model.enums.TypeIntervention;
import com.example.ttcarburant.repository.MaintenanceRepository;
import com.example.ttcarburant.repository.VehiculeRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepo;
    private final VehiculeRepository vehiculeRepo;

    public MaintenanceService(MaintenanceRepository maintenanceRepo,
                              VehiculeRepository vehiculeRepo) {
        this.maintenanceRepo = maintenanceRepo;
        this.vehiculeRepo = vehiculeRepo;
    }

    // ── CRUD Principal ────────────────────────────────────────────────────

    @Transactional
    public MaintenanceDto creer(MaintenanceRequest req) {
        Vehicule v = findVehicule(req.getVehiculeMatricule());

        Maintenance m = new Maintenance();
        m.setNumeroDossier(req.getNumeroDossier());
        m.setVehicule(v);
        m.setDateIntervention(req.getDateIntervention());
        m.setTypeIntervention(req.getTypeIntervention());
        m.setStatut(req.getStatut() != null ? req.getStatut() : StatutMaintenance.EN_COURS);
        m.setDescription(req.getDescription());
        m.setCreePar(getEmailCourant());

        // Ajouter les détails si fournis
        if (req.getDetails() != null) {
            for (DetailMaintenanceDto dr : req.getDetails()) {
                DetailMaintenance d = buildDetail(dr, m);
                m.getDetails().add(d);
            }
        }
        m.recalculerTotal();
        return toDto(maintenanceRepo.save(m), true);
    }

    @Transactional(readOnly = true)
    public MaintenanceDto getById(Long id) {
        return toDto(findMaintenance(id), true);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> getAll() {
        return maintenanceRepo.findAll().stream()
                .map(m -> toDto(m, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> getByVehicule(String matricule) {
        return maintenanceRepo.findByVehicule_MatriculeOrderByDateInterventionDesc(matricule)
                .stream().map(m -> toDto(m, true)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> getByZone(Long zoneId) {
        return maintenanceRepo.findByZoneId(zoneId)
                .stream().map(m -> toDto(m, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> getByStatut(StatutMaintenance statut) {
        return maintenanceRepo.findByStatutOrderByDateCreationDesc(statut)
                .stream().map(m -> toDto(m, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> getByType(TypeIntervention type) {
        return maintenanceRepo.findByTypeInterventionOrderByDateCreationDesc(type)
                .stream().map(m -> toDto(m, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> search(String query) {
        return maintenanceRepo.search(query)
                .stream().map(m -> toDto(m, false)).collect(Collectors.toList());
    }

    @Transactional
    public MaintenanceDto modifier(Long id, MaintenanceRequest req) {
        Maintenance m = findMaintenance(id);

        m.setNumeroDossier(req.getNumeroDossier());
        m.setDateIntervention(req.getDateIntervention());
        m.setTypeIntervention(req.getTypeIntervention());
        if (req.getStatut() != null) m.setStatut(req.getStatut());
        m.setDescription(req.getDescription());

        // Remplacer les détails si fournis
        if (req.getDetails() != null) {
            m.getDetails().clear();
            for (DetailMaintenanceDto dr : req.getDetails()) {
                m.getDetails().add(buildDetail(dr, m));
            }
        }
        m.recalculerTotal();
        return toDto(maintenanceRepo.save(m), true);
    }

    @Transactional
    public void supprimer(Long id) {
        if (!maintenanceRepo.existsById(id))
            throw new RuntimeException("Dossier de maintenance non trouvé : " + id);
        maintenanceRepo.deleteById(id);
    }

    // ── Gestion des détails ───────────────────────────────────────────────

    @Transactional
    public MaintenanceDto ajouterDetail(Long maintenanceId, DetailMaintenanceDto req) {
        Maintenance m = findMaintenance(maintenanceId);
        DetailMaintenance d = buildDetail(req, m);
        m.getDetails().add(d);
        m.recalculerTotal();
        return toDto(maintenanceRepo.save(m), true);
    }

    @Transactional
    public MaintenanceDto supprimerDetail(Long maintenanceId, Long detailId) {
        Maintenance m = findMaintenance(maintenanceId);
        m.getDetails().removeIf(d -> d.getId().equals(detailId));
        m.recalculerTotal();
        return toDto(maintenanceRepo.save(m), true);
    }

    // ── Vue "Global Vehicle List" ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGlobalVehicleList() {
        List<Maintenance> all = maintenanceRepo.findAll();

        Map<String, List<Maintenance>> byVehicule = all.stream()
                .collect(Collectors.groupingBy(m -> m.getVehicule().getMatricule()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Maintenance>> entry : byVehicule.entrySet()) {
            String matricule = entry.getKey();
            List<Maintenance> maintenances = entry.getValue();
            Vehicule v = maintenances.get(0).getVehicule();

            double totalHtva = maintenances.stream()
                    .mapToDouble(Maintenance::getCoutTotalHtva).sum();
            totalHtva = Math.round(totalHtva * 1000.0) / 1000.0;

            Set<String> marques = maintenances.stream()
                    .flatMap(m -> m.getDetails().stream())
                    .map(DetailMaintenance::getMarque)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .collect(Collectors.toCollection(TreeSet::new));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehiculeId", matricule);
            row.put("vehiculeMarque", v.getMarqueModele());
            row.put("zoneNom", v.getZone() != null ? v.getZone().getNom() : null);
            row.put("totalHtva", totalHtva);
            row.put("brands", String.join(", ", marques));
            row.put("nbDossiers", maintenances.size());

            result.add(row);
        }

        result.sort((a, b) -> Double.compare((double) b.get("totalHtva"), (double) a.get("totalHtva")));
        return result;
    }

    // ── Analytics ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        Object[] globalStats = maintenanceRepo.getGlobalStats();
        long nbDossiers = globalStats[0] != null ? ((Number) globalStats[0]).longValue() : 0;
        double totalHtva = globalStats[1] != null ? ((Number) globalStats[1]).doubleValue() : 0;

        List<Object[]> parType = maintenanceRepo.getStatsParType();
        List<Object[]> parVehicule = maintenanceRepo.getCoutParVehicule();
        List<Object[]> parZone = maintenanceRepo.getCoutParZone();
        List<Object[]> parPrestataire = maintenanceRepo.getCoutParPrestataire();

        List<Map<String, Object>> topVehicules = parVehicule.stream().limit(10).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("matricule", r[0]);
            m.put("totalHtva", Math.round(((Number) r[1]).doubleValue() * 1000.0) / 1000.0);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nbDossiers", nbDossiers);
        result.put("totalHtva", Math.round(totalHtva * 1000.0) / 1000.0);
        result.put("statsParType", buildTypeStats(parType));
        result.put("topVehicules", topVehicules);
        result.put("coutParZone", buildZoneStats(parZone));
        result.put("coutParPrestataire", buildPrestatStats(parPrestataire));
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private DetailMaintenance buildDetail(DetailMaintenanceDto dr, Maintenance m) {
        DetailMaintenance d = new DetailMaintenance();
        d.setMaintenance(m);
        d.setType(dr.getType());
        d.setNumeroDossier(dr.getNumeroDossier() != null ? dr.getNumeroDossier() : m.getNumeroDossier());
        d.setMarque(dr.getMarque());
        d.setNumero(dr.getNumero());
        d.setNumeroPiece(dr.getNumeroPiece());
        d.setDesignation(dr.getDesignation());
        d.setQuantite(dr.getQuantite() > 0 ? dr.getQuantite() : 1);
        d.setMontantUnitaire(dr.getMontantUnitaire());
        d.calculerMontant();
        return d;
    }

    private MaintenanceDto toDto(Maintenance m, boolean avecDetails) {
        MaintenanceDto dto = new MaintenanceDto();
        dto.setId(m.getId());
        dto.setNumeroDossier(m.getNumeroDossier());
        dto.setVehiculeMatricule(m.getVehicule().getMatricule());
        dto.setVehiculeMarqueModele(m.getVehicule().getMarqueModele());
        dto.setVehiculeZoneNom(m.getVehicule().getZone() != null
                ? m.getVehicule().getZone().getNom() : null);
        dto.setDateIntervention(m.getDateIntervention());
        dto.setTypeIntervention(m.getTypeIntervention());
        dto.setStatut(m.getStatut());
        dto.setDescription(m.getDescription());
        dto.setCoutTotalHtva(m.getCoutTotalHtva());
        dto.setCreePar(m.getCreePar());
        dto.setDateCreation(m.getDateCreation());
        dto.setNbDetails(m.getDetails().size());

        String brands = m.getDetails().stream()
                .map(DetailMaintenance::getMarque)
                .filter(Objects::nonNull)
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        dto.setBrands(brands);

        if (avecDetails) {
            List<DetailMaintenanceDto> detailDtos = m.getDetails().stream()
                    .map(this::toDetailDto)
                    .collect(Collectors.toList());
            dto.setDetails(detailDtos);
        }
        return dto;
    }

    private DetailMaintenanceDto toDetailDto(DetailMaintenance d) {
        DetailMaintenanceDto dto = new DetailMaintenanceDto();
        dto.setId(d.getId());
        dto.setType(d.getType());
        dto.setNumeroDossier(d.getNumeroDossier());
        dto.setMarque(d.getMarque());
        dto.setNumero(d.getNumero());
        dto.setNumeroPiece(d.getNumeroPiece());
        dto.setDesignation(d.getDesignation());
        dto.setQuantite(d.getQuantite());
        dto.setMontantUnitaire(d.getMontantUnitaire());
        dto.setTotalHtva(d.getTotalHtva());
        return dto;
    }

    private List<Map<String, Object>> buildTypeStats(List<Object[]> rows) {
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", r[0]);
            m.put("count", r[1]);
            m.put("totalHtva", r[2] != null ? Math.round(((Number) r[2]).doubleValue() * 100) / 100.0 : 0);
            return m;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildZoneStats(List<Object[]> rows) {
        return rows.stream().limit(10).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("zone", r[0]);
            m.put("totalHtva", r[1] != null ? Math.round(((Number) r[1]).doubleValue() * 100) / 100.0 : 0);
            return m;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPrestatStats(List<Object[]> rows) {
        return rows.stream().limit(10).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("prestataire", r[0]);
            m.put("totalHtva", r[1] != null ? Math.round(((Number) r[1]).doubleValue() * 100) / 100.0 : 0);
            return m;
        }).collect(Collectors.toList());
    }

    private Maintenance findMaintenance(Long id) {
        return maintenanceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier maintenance non trouvé : " + id));
    }

    private Vehicule findVehicule(String matricule) {
        return vehiculeRepo.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));
    }

    private String getEmailCourant() {
        try { return SecurityContextHolder.getContext().getAuthentication().getName(); }
        catch (Exception e) { return "system"; }
    }
}