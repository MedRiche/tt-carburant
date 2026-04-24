package com.example.ttcarburant.services;

import com.example.ttcarburant.dto.GroupeElectrogene.GestionCarburantGEDto;
import com.example.ttcarburant.dto.GroupeElectrogene.GestionCarburantGERequest;
import com.example.ttcarburant.model.entity.GestionCarburantGE;
import com.example.ttcarburant.model.entity.GroupeElectrogene;
import com.example.ttcarburant.model.enums.Semestre;
import com.example.ttcarburant.repository.GestionCarburantGERepository;
import com.example.ttcarburant.repository.GroupeElectrogeneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GestionCarburantGEService {

    private final GestionCarburantGERepository geRepo;
    private final GroupeElectrogeneRepository groupeRepo;

    public GestionCarburantGEService(GestionCarburantGERepository geRepo,
                                     GroupeElectrogeneRepository groupeRepo) {
        this.geRepo = geRepo;
        this.groupeRepo = groupeRepo;
    }

    @Transactional
    public GestionCarburantGEDto saisir(GestionCarburantGERequest req) {
        GroupeElectrogene ge = groupeRepo.findById(req.getSite())
                .orElseThrow(() -> new RuntimeException("Groupe électrogène non trouvé : " + req.getSite()));

        if (geRepo.findByGroupeElectrogeneAndAnneeAndSemestre(ge, req.getAnnee(), req.getSemestre()).isPresent()) {
            throw new RuntimeException("Saisie déjà existante pour ce semestre");
        }

        GestionCarburantGE entity = new GestionCarburantGE();
        entity.setGroupeElectrogene(ge);
        entity.setAnnee(req.getAnnee());
        entity.setSemestre(req.getSemestre());
        entity.setIndexHeureSemestrePrecedent(req.getIndexHeureSemestrePrecedent());
        entity.setMontantCarburantRestantReservoirPrecedent(req.getMontantCarburantRestantReservoirPrecedent());
        entity.setRavitaillementSemestrePrecedentDinars(req.getRavitaillementSemestrePrecedentDinars());
        entity.setMontantRestantAgilisFinSemestre(req.getMontantRestantAgilisFinSemestre());
        entity.setIndexFinSemestre(req.getIndexFinSemestre());

        calculerFormules(entity, ge);
        return toDto(geRepo.save(entity));
    }

    @Transactional
    public GestionCarburantGEDto modifier(Long id, GestionCarburantGERequest req) {
        GestionCarburantGE entity = geRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enregistrement non trouvé"));
        GroupeElectrogene ge = entity.getGroupeElectrogene();

        entity.setIndexHeureSemestrePrecedent(req.getIndexHeureSemestrePrecedent());
        entity.setMontantCarburantRestantReservoirPrecedent(req.getMontantCarburantRestantReservoirPrecedent());
        entity.setRavitaillementSemestrePrecedentDinars(req.getRavitaillementSemestrePrecedentDinars());
        entity.setMontantRestantAgilisFinSemestre(req.getMontantRestantAgilisFinSemestre());
        entity.setIndexFinSemestre(req.getIndexFinSemestre());

        calculerFormules(entity, ge);
        return toDto(geRepo.save(entity));
    }

    @Transactional
    public void supprimer(Long id) {
        GestionCarburantGE entity = geRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enregistrement non trouvé"));
        geRepo.delete(entity);
    }

    private void calculerFormules(GestionCarburantGE e, GroupeElectrogene ge) {
        Double prix = ge.getPrixCarburant();
        if (prix == null || prix <= 0) {
            throw new RuntimeException("Prix carburant non défini pour le groupe " + ge.getSite());
        }

        // Safely convert Double to double, using 0.0 for null values
        double ravitPrec = e.getRavitaillementSemestrePrecedentDinars() != null ? e.getRavitaillementSemestrePrecedentDinars() : 0.0;
        double montantPrec = e.getMontantCarburantRestantReservoirPrecedent() != null ? e.getMontantCarburantRestantReservoirPrecedent() : 0.0;
        double totalLitres = (ravitPrec + montantPrec) / prix;
        e.setTotalRavitaillementLitres(round3(totalLitres));

        double qteRestante = montantPrec / prix;
        e.setQuantiteRestanteReservoirAgilis(round3(qteRestante));

        double indexFin = e.getIndexFinSemestre() != null ? e.getIndexFinSemestre() : 0.0;
        double indexDebut = e.getIndexHeureSemestrePrecedent() != null ? e.getIndexHeureSemestrePrecedent() : 0.0;
        double heures = indexFin - indexDebut;
        e.setNbHeuresTravail(round3(heures));

        double pct = (heures > 0) ? (totalLitres - qteRestante) / heures : 0.0;
        e.setPourcentageConsommation(round3(pct));

        Double consoMax = ge.getConsommationTotaleMaxParSemestre();
        double budgetSemestre = (consoMax != null ? consoMax : 0.0) * prix;
        double demande = budgetSemestre - montantPrec;
        e.setCarburantDemandeDinarsCours(round3(Math.max(0, demande)));

        e.setEvaluationTauxConsommation(e.getPourcentageConsommation() != null && e.getPourcentageConsommation() > 1 ? "NON" : "OUI");
    }

    private double round3(double val) {
        return Math.round(val * 1000.0) / 1000.0;
    }

    private GestionCarburantGEDto toDto(GestionCarburantGE e) {
        GestionCarburantGEDto dto = new GestionCarburantGEDto();
        dto.setId(e.getId());
        dto.setSite(e.getGroupeElectrogene().getSite());
        if (e.getGroupeElectrogene().getZone() != null)
            dto.setSiteZoneNom(e.getGroupeElectrogene().getZone().getNom());
        dto.setAnnee(e.getAnnee());
        dto.setSemestre(e.getSemestre());
        dto.setIndexHeureSemestrePrecedent(e.getIndexHeureSemestrePrecedent());
        dto.setMontantCarburantRestantReservoirPrecedent(e.getMontantCarburantRestantReservoirPrecedent());
        dto.setRavitaillementSemestrePrecedentDinars(e.getRavitaillementSemestrePrecedentDinars());
        dto.setMontantRestantAgilisFinSemestre(e.getMontantRestantAgilisFinSemestre());
        dto.setIndexFinSemestre(e.getIndexFinSemestre());
        dto.setTotalRavitaillementLitres(e.getTotalRavitaillementLitres());
        dto.setQuantiteRestanteReservoirAgilis(e.getQuantiteRestanteReservoirAgilis());
        dto.setNbHeuresTravail(e.getNbHeuresTravail());
        dto.setPourcentageConsommation(e.getPourcentageConsommation());
        dto.setCarburantDemandeDinarsCours(e.getCarburantDemandeDinarsCours());
        dto.setEvaluationTauxConsommation(e.getEvaluationTauxConsommation());
        dto.setDateCreation(e.getDateCreation());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<GestionCarburantGEDto> getBySite(String site) {
        GroupeElectrogene ge = groupeRepo.findById(site)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));
        return geRepo.findByGroupeElectrogeneOrderByAnneeDescSemestreDesc(ge)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GestionCarburantGEDto> getByPeriode(int annee, Semestre semestre) {
        return geRepo.findByAnneeAndSemestreOrderByGroupeElectrogene_Site(annee, semestre)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GestionCarburantGEDto> getByZoneAndPeriode(Long zoneId, int annee, Semestre semestre) {
        return geRepo.findByZoneAndPeriode(zoneId, annee, semestre)
                .stream().map(this::toDto).collect(Collectors.toList());
    }
}