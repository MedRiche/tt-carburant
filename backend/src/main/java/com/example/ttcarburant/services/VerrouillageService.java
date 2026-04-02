package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.VerrouillageDto;
import com.example.ttcarburant.model.entity.VerrouillageCarburant;
import com.example.ttcarburant.model.entity.Zone;
import com.example.ttcarburant.repository.VerrouillageCarburantRepository;
import com.example.ttcarburant.repository.ZoneRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VerrouillageService {

    private static final String[] MOIS_LABELS = {
            "", "Janvier","Février","Mars","Avril","Mai","Juin",
            "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
    };

    private final VerrouillageCarburantRepository verrouillageRepo;
    private final ZoneRepository zoneRepo;

    public VerrouillageService(VerrouillageCarburantRepository verrouillageRepo,
                               ZoneRepository zoneRepo) {
        this.verrouillageRepo = verrouillageRepo;
        this.zoneRepo = zoneRepo;
    }

    // ── Vérifier si un mois est verrouillé ───────────────────────

    @Transactional(readOnly = true)
    public boolean isVerrouille(int annee, int mois, Long zoneId) {
        if (zoneId != null) {
            return verrouillageRepo.isVerrouille(annee, mois, zoneId);
        }
        // Vérifier le verrou global
        Optional<VerrouillageCarburant> global =
                verrouillageRepo.findByAnneeAndMoisAndZoneIsNull(annee, mois);
        return global.map(VerrouillageCarburant::isVerrouille).orElse(false);
    }

    @Transactional(readOnly = true)
    public VerrouillageDto getStatut(int annee, int mois, Long zoneId) {
        Optional<VerrouillageCarburant> opt;
        if (zoneId != null) {
            Zone zone = zoneRepo.findById(zoneId)
                    .orElseThrow(() -> new RuntimeException("Zone non trouvée"));
            opt = verrouillageRepo.findByAnneeAndMoisAndZone(annee, mois, zone);
        } else {
            opt = verrouillageRepo.findByAnneeAndMoisAndZoneIsNull(annee, mois);
        }

        if (opt.isPresent()) {
            return toDto(opt.get());
        }

        // Retourner un DTO avec verrouille=false si pas encore créé
        VerrouillageDto dto = new VerrouillageDto();
        dto.setAnnee(annee);
        dto.setMois(mois);
        dto.setMoisLabel(MOIS_LABELS[mois] + " " + annee);
        dto.setVerrouille(false);
        if (zoneId != null) {
            dto.setZoneId(zoneId);
            zoneRepo.findById(zoneId).ifPresent(z -> dto.setZoneNom(z.getNom()));
        }
        return dto;
    }

    // ── Verrouiller un mois ────────────────────────────────────────

    @Transactional
    public VerrouillageDto verrouiller(int annee, int mois, Long zoneId) {
        String emailAdmin = SecurityContextHolder.getContext().getAuthentication().getName();

        VerrouillageCarburant verrou;
        if (zoneId != null) {
            Zone zone = zoneRepo.findById(zoneId)
                    .orElseThrow(() -> new RuntimeException("Zone non trouvée"));
            verrou = verrouillageRepo.findByAnneeAndMoisAndZone(annee, mois, zone)
                    .orElseGet(() -> {
                        VerrouillageCarburant v = new VerrouillageCarburant();
                        v.setAnnee(annee);
                        v.setMois(mois);
                        v.setZone(zone);
                        return v;
                    });
        } else {
            verrou = verrouillageRepo.findByAnneeAndMoisAndZoneIsNull(annee, mois)
                    .orElseGet(() -> {
                        VerrouillageCarburant v = new VerrouillageCarburant();
                        v.setAnnee(annee);
                        v.setMois(mois);
                        return v;
                    });
        }

        verrou.setVerrouille(true);
        verrou.setVerrouilleParEmail(emailAdmin);
        verrou.setVerrouillerLe(LocalDateTime.now());
        verrou.setDeverrouilleParEmail(null);
        verrou.setDeverrouillerLe(null);

        return toDto(verrouillageRepo.save(verrou));
    }

    // ── Déverrouiller un mois ──────────────────────────────────────

    @Transactional
    public VerrouillageDto deverrouiller(int annee, int mois, Long zoneId) {
        String emailAdmin = SecurityContextHolder.getContext().getAuthentication().getName();

        VerrouillageCarburant verrou;
        if (zoneId != null) {
            Zone zone = zoneRepo.findById(zoneId)
                    .orElseThrow(() -> new RuntimeException("Zone non trouvée"));
            verrou = verrouillageRepo.findByAnneeAndMoisAndZone(annee, mois, zone)
                    .orElseThrow(() -> new RuntimeException("Aucun verrou trouvé pour ce mois/zone"));
        } else {
            verrou = verrouillageRepo.findByAnneeAndMoisAndZoneIsNull(annee, mois)
                    .orElseThrow(() -> new RuntimeException("Aucun verrou global trouvé pour ce mois"));
        }

        verrou.setVerrouille(false);
        verrou.setDeverrouilleParEmail(emailAdmin);
        verrou.setDeverrouillerLe(LocalDateTime.now());

        return toDto(verrouillageRepo.save(verrou));
    }

    // ── Lister les verrous ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VerrouillageDto> getVerrouxParAnnee(int annee) {
        return verrouillageRepo.findByAnneeOrderByMoisAsc(annee)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VerrouillageDto> getTousLesVerrouxActifs() {
        return verrouillageRepo.findByVerrouilleTrue()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Helper ────────────────────────────────────────────────────

    private VerrouillageDto toDto(VerrouillageCarburant v) {
        VerrouillageDto dto = new VerrouillageDto();
        dto.setId(v.getId());
        dto.setAnnee(v.getAnnee());
        dto.setMois(v.getMois());
        dto.setMoisLabel(MOIS_LABELS[v.getMois()] + " " + v.getAnnee());
        dto.setVerrouille(v.isVerrouille());
        dto.setVerrouilleParEmail(v.getVerrouilleParEmail());
        dto.setVerrouillerLe(v.getVerrouillerLe());
        dto.setDeverrouilleParEmail(v.getDeverrouilleParEmail());
        dto.setDeverrouillerLe(v.getDeverrouillerLe());
        if (v.getZone() != null) {
            dto.setZoneId(v.getZone().getId());
            dto.setZoneNom(v.getZone().getNom());
        }
        return dto;
    }
}