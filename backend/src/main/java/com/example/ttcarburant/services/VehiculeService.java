package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.VehiculeDto;
import com.example.ttcarburant.dto.VehiculeRequest;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.repository.VehiculeRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehiculeService {

    private final VehiculeRepository vehiculeRepository;
    private final ZoneRepository zoneRepository;

    public VehiculeService(VehiculeRepository vehiculeRepository, ZoneRepository zoneRepository) {
        this.vehiculeRepository = vehiculeRepository;
        this.zoneRepository = zoneRepository;
    }

    @Transactional(readOnly = true)
    public List<VehiculeDto> getAllVehicules() {
        return vehiculeRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehiculeDto> getVehiculesByZone(Long zoneId) {
        return vehiculeRepository.findByZone_Id(zoneId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehiculeDto getVehiculeById(String matricule) {
        Vehicule v = vehiculeRepository.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));
        return toDto(v);
    }

    @Transactional
    public VehiculeDto creerVehicule(VehiculeRequest req) {
        if (vehiculeRepository.existsByMatricule(req.getMatricule())) {
            throw new RuntimeException("Un véhicule avec ce matricule existe déjà");
        }
        Vehicule v = fromRequest(req, new Vehicule());
        return toDto(vehiculeRepository.save(v));
    }

    @Transactional
    public VehiculeDto modifierVehicule(String matricule, VehiculeRequest req) {
        Vehicule v = vehiculeRepository.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));
        fromRequest(req, v);
        return toDto(vehiculeRepository.save(v));
    }

    @Transactional
    public void supprimerVehicule(String matricule) {
        if (!vehiculeRepository.existsByMatricule(matricule)) {
            throw new RuntimeException("Véhicule non trouvé : " + matricule);
        }
        vehiculeRepository.deleteById(matricule);
    }

    @Transactional
    public VehiculeDto affecterZone(String matricule, Long zoneId) {
        Vehicule v = vehiculeRepository.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Zone non trouvée : " + zoneId));
        v.setZone(zone);
        return toDto(vehiculeRepository.save(v));
    }

    // ── Mapping helpers ──────────────────────────────────────────────────

    private Vehicule fromRequest(VehiculeRequest req, Vehicule v) {
        v.setMatricule(req.getMatricule());
        v.setDateMiseService(req.getDateMiseService());
        v.setMarqueModele(req.getMarqueModele());
        v.setTypeVehicule(req.getTypeVehicule());
        v.setSubdivision(req.getSubdivision());
        v.setCentre(req.getCentre());
        v.setResidenceService(req.getResidenceService());
        v.setNomConducteur(req.getNomConducteur());
        v.setPrenomConducteur(req.getPrenomConducteur());
        v.setTypeCarburant(req.getTypeCarburant());
        v.setPrixCarburant(req.getPrixCarburant());
        v.setIndexVidange(req.getIndexVidange());
        v.setVisiteTechnique(req.getVisiteTechnique());
        v.setIndexPneumatique(req.getIndexPneumatique());
        v.setKilometrageTotal(req.getKilometrageTotal());
        v.setConsommationDinarsCumul(req.getConsommationDinarsCumul());
        v.setConsommationLitresCumul(req.getConsommationLitresCumul());
        v.setCoutDuMois(req.getCoutDuMois());
        v.setCroxChaine(req.getCroxChaine());
        v.setIndexBatterie(req.getIndexBatterie());
        if (req.getZoneId() != null) {
            Zone zone = zoneRepository.findById(req.getZoneId())
                    .orElseThrow(() -> new RuntimeException("Zone non trouvée"));
            v.setZone(zone);
        }
        return v;
    }

    private VehiculeDto toDto(Vehicule v) {
        VehiculeDto dto = new VehiculeDto();
        dto.setMatricule(v.getMatricule());
        dto.setDateMiseService(v.getDateMiseService());
        dto.setMarqueModele(v.getMarqueModele());
        dto.setTypeVehicule(v.getTypeVehicule());
        dto.setSubdivision(v.getSubdivision());
        dto.setCentre(v.getCentre());
        dto.setResidenceService(v.getResidenceService());
        dto.setNomConducteur(v.getNomConducteur());
        dto.setPrenomConducteur(v.getPrenomConducteur());
        dto.setTypeCarburant(v.getTypeCarburant());
        dto.setPrixCarburant(v.getPrixCarburant());
        dto.setIndexVidange(v.getIndexVidange());
        dto.setVisiteTechnique(v.getVisiteTechnique());
        dto.setIndexPneumatique(v.getIndexPneumatique());
        dto.setKilometrageTotal(v.getKilometrageTotal());
        dto.setConsommationDinarsCumul(v.getConsommationDinarsCumul());
        dto.setConsommationLitresCumul(v.getConsommationLitresCumul());
        dto.setCoutDuMois(v.getCoutDuMois());
        dto.setCroxChaine(v.getCroxChaine());
        dto.setIndexBatterie(v.getIndexBatterie());
        if (v.getZone() != null) {
            dto.setZoneId(v.getZone().getId());
            dto.setZoneNom(v.getZone().getNom());
        }
        return dto;
    }
}