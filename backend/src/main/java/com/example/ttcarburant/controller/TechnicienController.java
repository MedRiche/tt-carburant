package com.example.ttcarburant.controller;

import com.example.ttcarburant.dto.ZoneDto;
import com.example.ttcarburant.dto.UtilisateurDto;
import com.example.ttcarburant.model.entity.AffectationUtilisateurZone;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.repository.AffectationUtilisateurZoneRepository;
import com.example.ttcarburant.repository.UtilisateurRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/technicien")
@PreAuthorize("hasRole('TECHNICIEN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class TechnicienController {

    private final UtilisateurRepository utilisateurRepository;
    private final AffectationUtilisateurZoneRepository affectationRepository;

    public TechnicienController(UtilisateurRepository utilisateurRepository,
                                AffectationUtilisateurZoneRepository affectationRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.affectationRepository = affectationRepository;
    }

    /**
     * Récupère le profil du technicien connecté avec ses zones affectées
     */
    @GetMapping("/profil")
    public ResponseEntity<?> getProfil() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Utilisateur technicien = utilisateurRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<AffectationUtilisateurZone> affectations =
                    affectationRepository.findByUtilisateur(technicien);

            List<ZoneDto> zones = affectations.stream()
                    .map(aff -> {
                        ZoneDto dto = new ZoneDto();
                        dto.setId(aff.getZone().getId());
                        dto.setNom(aff.getZone().getNom());
                        dto.setDescription(aff.getZone().getDescription());
                        dto.setResponsable(aff.getZone().getResponsable());
                        dto.setDateCreation(aff.getZone().getDateCreation());
                        return dto;
                    })
                    .collect(Collectors.toList());

            UtilisateurDto dto = new UtilisateurDto();
            dto.setId(technicien.getId());
            dto.setNom(technicien.getNom());
            dto.setEmail(technicien.getEmail());
            dto.setRole(technicien.getRole());
            dto.setStatutCompte(technicien.getStatutCompte());
            dto.setDateCreation(technicien.getDateCreation());
            dto.setSpecialite(technicien.getSpecialite());
            dto.setZones(zones);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Récupère uniquement les zones affectées au technicien connecté
     */
    @GetMapping("/mes-zones")
    public ResponseEntity<?> getMesZones() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Utilisateur technicien = utilisateurRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<AffectationUtilisateurZone> affectations =
                    affectationRepository.findByUtilisateur(technicien);

            List<ZoneDto> zones = affectations.stream()
                    .map(aff -> {
                        ZoneDto dto = new ZoneDto();
                        dto.setId(aff.getZone().getId());
                        dto.setNom(aff.getZone().getNom());
                        dto.setDescription(aff.getZone().getDescription());
                        dto.setResponsable(aff.getZone().getResponsable());
                        dto.setDateCreation(aff.getZone().getDateCreation());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(zones);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    private record ErrorResponse(String message) {}
}