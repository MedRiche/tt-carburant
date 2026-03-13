package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.ZoneDto;
import com.example.ttcarburant.dto.ZoneRequest;
import com.example.ttcarburant.model.entity.AffectationUtilisateurZone;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.repository.AffectationUtilisateurZoneRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final AffectationUtilisateurZoneRepository affectationRepository;

    public ZoneService(ZoneRepository zoneRepository,
                       AffectationUtilisateurZoneRepository affectationRepository) {
        this.zoneRepository = zoneRepository;
        this.affectationRepository = affectationRepository;
    }

    /**
     * Créer une nouvelle zone
     */
    @Transactional
    public ZoneDto creerZone(ZoneRequest request) {
        if (zoneRepository.existsByNom(request.getNom())) {
            throw new RuntimeException("Une zone avec ce nom existe déjà");
        }

        Zone zone = new Zone();
        zone.setNom(request.getNom());
        zone.setDescription(request.getDescription());
        zone.setResponsable(request.getResponsable());

        zone = zoneRepository.save(zone);
        return convertToDto(zone);
    }

    /**
     * Récupérer toutes les zones
     */
    @Transactional(readOnly = true)
    public List<ZoneDto> getAllZones() {
        return zoneRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer une zone par ID
     */
    @Transactional(readOnly = true)
    public ZoneDto getZoneById(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone non trouvée"));
        return convertToDto(zone);
    }

    /**
     * Modifier une zone
     */
    @Transactional
    public ZoneDto modifierZone(Long id, ZoneRequest request) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone non trouvée"));

        // Vérifier si le nouveau nom existe déjà (sauf si c'est le même)
        if (!zone.getNom().equals(request.getNom()) && zoneRepository.existsByNom(request.getNom())) {
            throw new RuntimeException("Une zone avec ce nom existe déjà");
        }

        zone.setNom(request.getNom());
        zone.setDescription(request.getDescription());
        zone.setResponsable(request.getResponsable());

        zone = zoneRepository.save(zone);
        return convertToDto(zone);
    }

    /**
     * Supprimer une zone
     */
    @Transactional
    public void supprimerZone(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone non trouvée"));

        // Vérifier si la zone a des utilisateurs affectés
        List<AffectationUtilisateurZone> affectations = affectationRepository.findByZone(zone);
        if (!affectations.isEmpty()) {
            throw new RuntimeException("Impossible de supprimer une zone qui a des utilisateurs affectés");
        }

        zoneRepository.delete(zone);
    }

    /**
     * Convertir une entité Zone en DTO
     */
    private ZoneDto convertToDto(Zone zone) {
        int nombreUtilisateurs = affectationRepository.findByZone(zone).size();

        ZoneDto dto = new ZoneDto();
        dto.setId(zone.getId());
        dto.setNom(zone.getNom());
        dto.setDescription(zone.getDescription());
        dto.setResponsable(zone.getResponsable());
        dto.setDateCreation(zone.getDateCreation());
        dto.setNombreUtilisateurs(nombreUtilisateurs);

        return dto;
    }
}