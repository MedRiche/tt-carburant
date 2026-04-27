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

    private final VehiculeRepository             vehiculeRepository;
    private final ZoneRepository                 zoneRepository;
    private final ConducteurUserCreationService  conducteurService;

    public VehiculeService(VehiculeRepository vehiculeRepository,
                           ZoneRepository zoneRepository,
                           ConducteurUserCreationService conducteurService) {
        this.vehiculeRepository = vehiculeRepository;
        this.zoneRepository     = zoneRepository;
        this.conducteurService  = conducteurService;
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

    /**
     * Crée un véhicule et génère automatiquement un compte conducteur
     * si prénom + nom conducteur sont renseignés.
     */
    @Transactional
    public VehiculeDto creerVehicule(VehiculeRequest req) {
        if (vehiculeRepository.existsByMatricule(req.getMatricule())) {
            throw new RuntimeException("Un véhicule avec ce matricule existe déjà");
        }
        Vehicule v = fromRequest(req, new Vehicule());

        // ── Création automatique du compte conducteur ──────────────────────
        creerCompteConducteurSiPresent(req.getPrenomConducteur(), req.getNomConducteur());

        return toDto(vehiculeRepository.save(v));
    }

    /**
     * Modifie un véhicule et crée le compte conducteur si le nom/prénom
     * a changé et que le nouveau conducteur n'a pas encore de compte.
     */
    @Transactional
    public VehiculeDto modifierVehicule(String matricule, VehiculeRequest req) {
        Vehicule v = vehiculeRepository.findById(matricule)
                .orElseThrow(() -> new RuntimeException("Véhicule non trouvé : " + matricule));

        String ancienPrenom = v.getPrenomConducteur();
        String ancienNom    = v.getNomConducteur();

        fromRequest(req, v);

        // Créer le compte si le conducteur a changé
        boolean prenomChange = !equalsIgnoreCaseNull(ancienPrenom, req.getPrenomConducteur());
        boolean nomChange    = !equalsIgnoreCaseNull(ancienNom,    req.getNomConducteur());

        if (prenomChange || nomChange) {
            creerCompteConducteurSiPresent(req.getPrenomConducteur(), req.getNomConducteur());
        }

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

    // ── Helpers privés ───────────────────────────────────────────────────────

    /**
     * Crée un compte TECHNICIEN (EN_ATTENTE) pour le conducteur
     * uniquement si au moins le prénom ou le nom est renseigné.
     */
    private void creerCompteConducteurSiPresent(String prenomConducteur, String nomConducteur) {
        boolean hasPrenom = prenomConducteur != null && !prenomConducteur.trim().isEmpty();
        boolean hasNom    = nomConducteur    != null && !nomConducteur.trim().isEmpty();
        if (hasPrenom || hasNom) {
            conducteurService.creerCompteConducteurUnique(
                    hasPrenom ? prenomConducteur.trim() : "",
                    hasNom    ? nomConducteur.trim()    : "");
        }
    }

    private boolean equalsIgnoreCaseNull(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

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