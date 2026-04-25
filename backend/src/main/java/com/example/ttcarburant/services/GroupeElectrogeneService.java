package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.GroupeElectrogene.GroupeElectrogeneDto;
import com.example.ttcarburant.model.entity.GroupeElectrogene;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.model.enums.TypeCarburant;
import com.example.ttcarburant.repository.GroupeElectrogeneRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupeElectrogeneService {

    private static final Logger log = LoggerFactory.getLogger(GroupeElectrogeneService.class);

    private final GroupeElectrogeneRepository geRepo;
    private final ZoneRepository zoneRepo;

    public GroupeElectrogeneService(GroupeElectrogeneRepository geRepo,
                                    ZoneRepository zoneRepo) {
        this.geRepo   = geRepo;
        this.zoneRepo = zoneRepo;
    }

    // ── READ ──────────────────────────────────────────────────────────────

    /**
     * FIX : on itère manuellement pour attraper les erreurs par ligne
     * et ne pas faire planter toute la liste si une entité est corrompue.
     */
    @Transactional(readOnly = true)
    public List<GroupeElectrogeneDto> getAll() {
        List<GroupeElectrogene> entities;
        try {
            entities = geRepo.findAll();
            log.info("getAll() → {} entités trouvées en base", entities.size());
        } catch (Exception e) {
            log.error("Erreur lors du findAll() groupes électrogènes", e);
            throw new RuntimeException("Impossible de lire les groupes électrogènes : " + e.getMessage(), e);
        }

        List<GroupeElectrogeneDto> dtos = new ArrayList<>();
        for (GroupeElectrogene ge : entities) {
            try {
                dtos.add(toDto(ge));
            } catch (Exception e) {
                // On logue l'entité corrompue mais on continue
                log.error("Erreur de mapping pour le site '{}' : {}", ge.getSite(), e.getMessage(), e);
            }
        }
        log.info("getAll() → {} DTOs mappés avec succès", dtos.size());
        return dtos;
    }

    @Transactional(readOnly = true)
    public GroupeElectrogeneDto getBySite(String site) {
        GroupeElectrogene ge = geRepo.findBySite(site)
                .orElseThrow(() -> new RuntimeException("Groupe électrogène non trouvé : " + site));
        return toDto(ge);
    }

    @Transactional(readOnly = true)
    public List<GroupeElectrogeneDto> getByZone(Long zoneId) {
        return geRepo.findByZoneId(zoneId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── WRITE ─────────────────────────────────────────────────────────────

    @Transactional
    public GroupeElectrogeneDto creer(GroupeElectrogeneDto req) {
        if (req.getSite() == null || req.getSite().isBlank()) {
            throw new RuntimeException("Le site est obligatoire");
        }
        if (geRepo.existsBySite(req.getSite())) {
            throw new RuntimeException("Un groupe électrogène existe déjà pour le site : " + req.getSite());
        }
        GroupeElectrogene ge = new GroupeElectrogene();
        mapDto(req, ge);
        GroupeElectrogene saved = geRepo.save(ge);
        log.info("Groupe électrogène créé : {}", saved.getSite());
        return toDto(saved);
    }

    @Transactional
    public GroupeElectrogeneDto modifier(String site, GroupeElectrogeneDto req) {
        GroupeElectrogene ge = geRepo.findBySite(site)
                .orElseThrow(() -> new RuntimeException("Groupe électrogène non trouvé : " + site));
        mapDto(req, ge);
        return toDto(geRepo.save(ge));
    }

    @Transactional
    public void supprimer(String site) {
        GroupeElectrogene ge = geRepo.findBySite(site)
                .orElseThrow(() -> new RuntimeException("Groupe électrogène non trouvé : " + site));
        geRepo.delete(ge);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────

    private void mapDto(GroupeElectrogeneDto dto, GroupeElectrogene ge) {
        ge.setSite(dto.getSite());

        // Type carburant (défaut GASOIL si null)
        ge.setTypeCarburant(
                dto.getTypeCarburant() != null
                        ? dto.getTypeCarburant()
                        : TypeCarburant.GASOIL_ORDINAIRE
        );

        ge.setPuissanceKVA(dto.getPuissanceKVA());
        ge.setTauxConsommationParHeure(dto.getTauxConsommationParHeure());
        ge.setConsommationTotaleMaxParSemestre(dto.getConsommationTotaleMaxParSemestre());
        ge.setPrixCarburant(dto.getPrixCarburant());

        // Carte Agilis
        ge.setTypeCarte(dto.getTypeCarte());
        ge.setNumeroCarte(dto.getNumeroCarte());
        ge.setCodePIN(dto.getCodePIN());
        ge.setCodePUK(dto.getCodePUK());
        ge.setUtilisateurRoc(dto.getUtilisateurRoc());

        // Date expiration : accepte "yyyy-MM" (input[type=month]) et "MM/yyyy"
        ge.setDateExpiration(parseDate(dto.getDateExpiration()));

        // Zone
        if (dto.getZoneId() != null) {
            Zone zone = zoneRepo.findById(dto.getZoneId()).orElse(null);
            ge.setZone(zone);
        } else {
            ge.setZone(null);
        }
    }

    /**
     * FIX PRINCIPAL : toDto() sécurisé.
     * - typeCarburant null → GASOIL_ORDINAIRE par défaut (évite NPE)
     * - dateExpiration null → pas de formatage (évite NPE)
     * - zone null → zoneId et zoneNom laissés null
     */
    GroupeElectrogeneDto toDto(GroupeElectrogene ge) {
        if (ge == null) {
            throw new IllegalArgumentException("GroupeElectrogene ne peut pas être null");
        }

        GroupeElectrogeneDto dto = new GroupeElectrogeneDto();
        dto.setSite(ge.getSite());

        // FIX : typeCarburant peut être null si la colonne est corrompue en base
        dto.setTypeCarburant(
                ge.getTypeCarburant() != null
                        ? ge.getTypeCarburant()
                        : TypeCarburant.GASOIL_ORDINAIRE
        );

        dto.setPuissanceKVA(ge.getPuissanceKVA());
        dto.setTauxConsommationParHeure(ge.getTauxConsommationParHeure());
        dto.setConsommationTotaleMaxParSemestre(ge.getConsommationTotaleMaxParSemestre());
        dto.setPrixCarburant(ge.getPrixCarburant());
        dto.setTypeCarte(ge.getTypeCarte());
        dto.setNumeroCarte(ge.getNumeroCarte());
        dto.setCodePIN(ge.getCodePIN());
        dto.setCodePUK(ge.getCodePUK());
        dto.setUtilisateurRoc(ge.getUtilisateurRoc());

        // FIX : dateExpiration peut être null
        if (ge.getDateExpiration() != null) {
            try {
                dto.setDateExpiration(
                        ge.getDateExpiration().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                );
            } catch (Exception e) {
                log.warn("Impossible de formater la date d'expiration pour le site '{}' : {}",
                        ge.getSite(), e.getMessage());
                dto.setDateExpiration(null);
            }
        }

        // FIX : zone peut être null (lazy loading ou absent)
        if (ge.getZone() != null) {
            try {
                dto.setZoneId(ge.getZone().getId());
                dto.setZoneNom(ge.getZone().getNom());
            } catch (Exception e) {
                log.warn("Impossible de lire la zone pour le site '{}' : {}",
                        ge.getSite(), e.getMessage());
            }
        }

        return dto;
    }

    /**
     * Convertit la date expiration String vers LocalDate.
     * Formats supportés :
     *  - "yyyy-MM"   → venant de input[type=month] HTML
     *  - "MM/yyyy"   → venant du fichier Excel
     */
    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();
        try {
            if (raw.contains("-")) {
                return java.time.YearMonth
                        .parse(raw, DateTimeFormatter.ofPattern("yyyy-MM"))
                        .atDay(1);
            } else {
                return java.time.YearMonth
                        .parse(raw, DateTimeFormatter.ofPattern("MM/yyyy"))
                        .atDay(1);
            }
        } catch (Exception e) {
            log.warn("Date expiration invalide ignorée : '{}'", raw);
            return null;
        }
    }
}