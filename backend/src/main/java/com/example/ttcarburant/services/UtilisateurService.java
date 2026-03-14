package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.UtilisateurDto;
import com.example.ttcarburant.dto.ValiderCompteRequest;
import com.example.ttcarburant.dto.ZoneDto;
import com.example.ttcarburant.model.entity.AffectationUtilisateurZone;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.model.enums.StatutCompte;
import com.example.ttcarburant.repository.AffectationUtilisateurZoneRepository;
import com.example.ttcarburant.repository.UtilisateurRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final ZoneRepository zoneRepository;
    private final AffectationUtilisateurZoneRepository affectationRepository;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              ZoneRepository zoneRepository,
                              AffectationUtilisateurZoneRepository affectationRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.zoneRepository = zoneRepository;
        this.affectationRepository = affectationRepository;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDto> getUtilisateursEnAttente() {
        return utilisateurRepository
                .findByStatutCompteOrderByDateCreationDesc(StatutCompte.EN_ATTENTE)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDto> getAllUtilisateurs() {
        return utilisateurRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UtilisateurDto getUtilisateurById(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return convertToDto(utilisateur);
    }

    @Transactional
    public UtilisateurDto validerCompteAvecZones(ValiderCompteRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(request.getUtilisateurId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (utilisateur.getStatutCompte() != StatutCompte.EN_ATTENTE) {
            throw new RuntimeException("Ce compte n'est pas en attente de validation");
        }

        String emailAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur admin = utilisateurRepository.findByEmail(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        utilisateur.setStatutCompte(StatutCompte.ACTIF);
        utilisateurRepository.save(utilisateur);

        for (Long zoneId : request.getZoneIds()) {
            Zone zone = zoneRepository.findById(zoneId)
                    .orElseThrow(() -> new RuntimeException("Zone non trouvée: " + zoneId));
            if (!affectationRepository.existsByUtilisateurAndZone(utilisateur, zone)) {
                affectationRepository.save(new AffectationUtilisateurZone(utilisateur, zone, admin));
            }
        }

        return convertToDto(utilisateur);
    }

    @Transactional
    public UtilisateurDto refuserCompte(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (utilisateur.getStatutCompte() != StatutCompte.EN_ATTENTE) {
            throw new RuntimeException("Ce compte n'est pas en attente de validation");
        }

        utilisateur.setStatutCompte(StatutCompte.REFUSE);
        utilisateurRepository.save(utilisateur);
        return convertToDto(utilisateur);
    }

    /**
     * TOGGLE:
     *   ACTIF     → DESACTIVE
     *   DESACTIVE → ACTIF
     *   REFUSE    → ACTIF   (réactivation depuis un refus)
     */
    @Transactional
    public UtilisateurDto toggleActivation(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        switch (utilisateur.getStatutCompte()) {
            case ACTIF:
                utilisateur.setStatutCompte(StatutCompte.DESACTIVE);
                break;
            case DESACTIVE:
            case REFUSE:
                utilisateur.setStatutCompte(StatutCompte.ACTIF);
                break;
            default:
                throw new RuntimeException("Action impossible pour un compte EN_ATTENTE");
        }

        utilisateurRepository.save(utilisateur);
        return convertToDto(utilisateur);
    }

    @Transactional
    public void supprimerUtilisateur(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        affectationRepository.deleteByUtilisateur(utilisateur);
        utilisateurRepository.deleteById(id);
    }

    @Transactional
    public UtilisateurDto ajouterZone(Long utilisateurId, Long zoneId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Zone non trouvée"));

        if (affectationRepository.existsByUtilisateurAndZone(utilisateur, zone)) {
            throw new RuntimeException("Cet utilisateur est déjà affecté à cette zone");
        }

        String emailAdmin = SecurityContextHolder.getContext().getAuthentication().getName();
        Utilisateur admin = utilisateurRepository.findByEmail(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        affectationRepository.save(new AffectationUtilisateurZone(utilisateur, zone, admin));
        return convertToDto(utilisateur);
    }

    @Transactional
    public UtilisateurDto retirerZone(Long utilisateurId, Long zoneId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Zone non trouvée"));
        affectationRepository.deleteByUtilisateurAndZone(utilisateur, zone);
        return convertToDto(utilisateur);
    }

    private UtilisateurDto convertToDto(Utilisateur utilisateur) {
        List<AffectationUtilisateurZone> affectations =
                affectationRepository.findByUtilisateur(utilisateur);

        List<ZoneDto> zones = affectations.stream()
                .map(aff -> {
                    Zone z = aff.getZone();
                    ZoneDto dto = new ZoneDto();
                    dto.setId(z.getId());
                    dto.setNom(z.getNom());
                    dto.setDescription(z.getDescription());
                    dto.setResponsable(z.getResponsable());
                    dto.setDateCreation(z.getDateCreation());
                    return dto;
                })
                .collect(Collectors.toList());

        UtilisateurDto dto = new UtilisateurDto();
        dto.setId(utilisateur.getId());
        dto.setNom(utilisateur.getNom());
        dto.setEmail(utilisateur.getEmail());
        dto.setRole(utilisateur.getRole());
        dto.setStatutCompte(utilisateur.getStatutCompte());
        dto.setDateCreation(utilisateur.getDateCreation());
        dto.setSpecialite(utilisateur.getSpecialite());
        dto.setZones(zones);
        return dto;
    }
}