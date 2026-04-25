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
        mapRequest(entity, req);
        calculerFormules(entity, ge);
        return toDto(geRepo.save(entity));
    }

    @Transactional
    public GestionCarburantGEDto modifier(Long id, GestionCarburantGERequest req) {
        GestionCarburantGE entity = geRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enregistrement non trouvé"));
        GroupeElectrogene ge = entity.getGroupeElectrogene();
        mapRequest(entity, req);
        calculerFormules(entity, ge);
        return toDto(geRepo.save(entity));
    }

    @Transactional
    public void supprimer(Long id) {
        GestionCarburantGE entity = geRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enregistrement non trouvé"));
        geRepo.delete(entity);
    }

    /**
     * Récupère TOUTES les saisies (tous sites, toutes périodes).
     * Utilisé par le frontend pour afficher l'historique dans la liste des groupes.
     */
    @Transactional(readOnly = true)
    public List<GestionCarburantGEDto> getAll() {
        return geRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
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

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void mapRequest(GestionCarburantGE e, GestionCarburantGERequest req) {
        e.setIndexHeureSemestrePrecedent(req.getIndexHeureSemestrePrecedent());
        e.setMontantCarburantRestantReservoirPrecedent(req.getMontantCarburantRestantReservoirPrecedent());
        e.setRavitaillementSemestrePrecedentDinars(req.getRavitaillementSemestrePrecedentDinars());
        e.setMontantRestantAgilisFinSemestre(req.getMontantRestantAgilisFinSemestre());
        e.setIndexFinSemestre(req.getIndexFinSemestre());
    }

    /**
     * Formules DAF issues du fichier Excel :
     *
     *   (1) Index heure début semestre
     *   (2) Montant restant réservoir précédent (DT)
     *   (3) Ravitaillement semestre précédent (DT)
     *   (4) Montant restant Agilis fin semestre (DT)
     *   (5) Index fin semestre
     *   (6) Nb heures travail = (5) - (1)
     *
     *   Total ravitaillement (L)       = ((2) + (3)) / prix
     *   Qté restante réservoir (L)     = (4) / prix
     *   Nb heures travail              = (5) - (1)
     *   % consommation                 = ((2) + (3) - (4)) * 100 / (6)   ← ×100 manquait !
     *   Carburant demandé (DT)         = (ConsoMax × prix) - (2)
     *   Évaluation taux consommation   = "OUI" si % ≤ 100, "NON" sinon
     */
    private void calculerFormules(GestionCarburantGE e, GroupeElectrogene ge) {
        Double prix = ge.getPrixCarburant();
        if (prix == null || prix <= 0) {
            // Prix non encore défini : on calcule ce qu'on peut sans planter
            prix = null;
        }

        double montantPrec = orZero(e.getMontantCarburantRestantReservoirPrecedent()); // (2)
        double ravitPrec   = orZero(e.getRavitaillementSemestrePrecedentDinars());     // (3)
        double montantFin  = orZero(e.getMontantRestantAgilisFinSemestre());           // (4)
        double indexDebut  = orZero(e.getIndexHeureSemestrePrecedent());               // (1)
        double indexFin    = orZero(e.getIndexFinSemestre());                          // (5)

        // (6) Nb heures travail
        double heures = indexFin - indexDebut;
        e.setNbHeuresTravail(round3(heures));

        if (prix != null) {
            // Total ravitaillement en litres = ((2) + (3)) / prix
            double totalLitres = (montantPrec + ravitPrec) / prix;
            e.setTotalRavitaillementLitres(round3(totalLitres));

            // Quantité restante réservoir + carte agilis fin semestre = (4) / prix
            double qteRestante = montantFin / prix;
            e.setQuantiteRestanteReservoirAgilis(round3(qteRestante));

            // % consommation = ((2) + (3) - (4)) * 100 / (6)
            // CORRECTION : multiplication par 100 était absente dans la version originale
            double pct = (heures > 0)
                    ? (montantPrec + ravitPrec - montantFin) * 100.0 / heures
                    : 0.0;
            e.setPourcentageConsommation(round3(pct));

            // Carburant demandé en DT = (ConsoMax × prix) - (2)
            Double consoMax = ge.getConsommationTotaleMaxParSemestre();
            double budgetSemestre = (consoMax != null ? consoMax : 0.0) * prix;
            double demande = budgetSemestre - montantPrec;
            e.setCarburantDemandeDinarsCours(round3(Math.max(0, demande)));

            // Évaluation : OUI si taux ≤ 100 %
            e.setEvaluationTauxConsommation(pct <= 100.0 ? "OUI" : "NON");
        } else {
            // Prix manquant : champs calculés à null, on ne lève pas d'exception
            e.setTotalRavitaillementLitres(null);
            e.setQuantiteRestanteReservoirAgilis(null);
            e.setPourcentageConsommation(null);
            e.setCarburantDemandeDinarsCours(null);
            e.setEvaluationTauxConsommation(null);
        }
    }

    private double orZero(Double v) { return v != null ? v : 0.0; }

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
}