package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.GroupeElectrogene.GroupeElectrogeneDto;
import com.example.ttcarburant.model.entity.GroupeElectrogene;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.model.enums.TypeCarburant;
import com.example.ttcarburant.repository.GroupeElectrogeneRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupeElectrogeneService {

    private final GroupeElectrogeneRepository geRepo;
    private final ZoneRepository zoneRepo;

    public GroupeElectrogeneService(GroupeElectrogeneRepository geRepo,
                                    ZoneRepository zoneRepo) {
        this.geRepo = geRepo;
        this.zoneRepo = zoneRepo;
    }

    @Transactional(readOnly = true)
    public List<GroupeElectrogeneDto> getAll() {
        return geRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
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
        return toDto(geRepo.save(ge));
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

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void mapDto(GroupeElectrogeneDto dto, GroupeElectrogene ge) {
        ge.setSite(dto.getSite());

        if (dto.getTypeCarburant() != null) {
            ge.setTypeCarburant(dto.getTypeCarburant());
        } else {
            ge.setTypeCarburant(TypeCarburant.GASOIL_ORDINAIRE);
        }

        ge.setPuissanceKVA(dto.getPuissanceKVA());
        ge.setTauxConsommationParHeure(dto.getTauxConsommationParHeure());
        ge.setConsommationTotaleMaxParSemestre(dto.getConsommationTotaleMaxParSemestre());
        ge.setPrixCarburant(dto.getPrixCarburant());
        ge.setTypeCarte(dto.getTypeCarte());
        ge.setNumeroCarte(dto.getNumeroCarte());
        ge.setCodePIN(dto.getCodePIN());
        ge.setCodePUK(dto.getCodePUK());
        ge.setUtilisateurRoc(dto.getUtilisateurRoc());

        // Conversion dateExpiration String -> LocalDate
        if (dto.getDateExpiration() != null && !dto.getDateExpiration().isBlank()) {
            try {
                // Accepte "yyyy-MM" (format HTML input[type=month]) et "MM/yyyy"
                String dateStr = dto.getDateExpiration().trim();
                LocalDate date;
                if (dateStr.contains("-")) {
                    // Format HTML : "2028-08"
                    date = java.time.YearMonth.parse(dateStr,
                            DateTimeFormatter.ofPattern("yyyy-MM")).atDay(1);
                } else {
                    // Format Excel : "08/2028"
                    date = java.time.YearMonth.parse(dateStr,
                            DateTimeFormatter.ofPattern("MM/yyyy")).atDay(1);
                }
                ge.setDateExpiration(date);
            } catch (Exception e) {
                ge.setDateExpiration(null);
            }
        } else {
            ge.setDateExpiration(null);
        }

        // Zone
        if (dto.getZoneId() != null) {
            Zone zone = zoneRepo.findById(dto.getZoneId()).orElse(null);
            ge.setZone(zone);
        } else {
            ge.setZone(null);
        }
    }

    private GroupeElectrogeneDto toDto(GroupeElectrogene ge) {
        GroupeElectrogeneDto dto = new GroupeElectrogeneDto();
        dto.setSite(ge.getSite());
        dto.setTypeCarburant(ge.getTypeCarburant());
        dto.setPuissanceKVA(ge.getPuissanceKVA());
        dto.setTauxConsommationParHeure(ge.getTauxConsommationParHeure());
        dto.setConsommationTotaleMaxParSemestre(ge.getConsommationTotaleMaxParSemestre());
        dto.setPrixCarburant(ge.getPrixCarburant());
        dto.setTypeCarte(ge.getTypeCarte());
        dto.setNumeroCarte(ge.getNumeroCarte());
        dto.setCodePIN(ge.getCodePIN());
        dto.setCodePUK(ge.getCodePUK());
        dto.setUtilisateurRoc(ge.getUtilisateurRoc());

        if (ge.getDateExpiration() != null) {
            // Renvoyer au format "yyyy-MM" pour être compatible avec input[type=month]
            dto.setDateExpiration(ge.getDateExpiration()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        if (ge.getZone() != null) {
            dto.setZoneId(ge.getZone().getId());
            dto.setZoneNom(ge.getZone().getNom());
        }

        return dto;
    }
}