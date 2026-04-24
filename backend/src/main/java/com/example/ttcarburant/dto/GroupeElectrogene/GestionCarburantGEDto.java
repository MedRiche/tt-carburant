package com.example.ttcarburant.dto.GroupeElectrogene;


import com.example.ttcarburant.model.enums.Semestre;
import java.time.LocalDateTime;

public class GestionCarburantGEDto {

    private Long id;
    private String site;
    private String siteZoneNom;
    private int annee;
    private Semestre semestre;
    private Double indexHeureSemestrePrecedent;
    private Double montantCarburantRestantReservoirPrecedent;
    private Double ravitaillementSemestrePrecedentDinars;
    private Double montantRestantAgilisFinSemestre;
    private Double indexFinSemestre;
    private Double totalRavitaillementLitres;
    private Double quantiteRestanteReservoirAgilis;
    private Double nbHeuresTravail;
    private Double pourcentageConsommation;
    private Double carburantDemandeDinarsCours;
    private String evaluationTauxConsommation;
    private LocalDateTime dateCreation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getSiteZoneNom() {
        return siteZoneNom;
    }

    public void setSiteZoneNom(String siteZoneNom) {
        this.siteZoneNom = siteZoneNom;
    }

    public int getAnnee() {
        return annee;
    }

    public void setAnnee(int annee) {
        this.annee = annee;
    }

    public Semestre getSemestre() {
        return semestre;
    }

    public void setSemestre(Semestre semestre) {
        this.semestre = semestre;
    }

    public Double getIndexHeureSemestrePrecedent() {
        return indexHeureSemestrePrecedent;
    }

    public void setIndexHeureSemestrePrecedent(Double indexHeureSemestrePrecedent) {
        this.indexHeureSemestrePrecedent = indexHeureSemestrePrecedent;
    }

    public Double getMontantCarburantRestantReservoirPrecedent() {
        return montantCarburantRestantReservoirPrecedent;
    }

    public void setMontantCarburantRestantReservoirPrecedent(Double montantCarburantRestantReservoirPrecedent) {
        this.montantCarburantRestantReservoirPrecedent = montantCarburantRestantReservoirPrecedent;
    }

    public Double getRavitaillementSemestrePrecedentDinars() {
        return ravitaillementSemestrePrecedentDinars;
    }

    public void setRavitaillementSemestrePrecedentDinars(Double ravitaillementSemestrePrecedentDinars) {
        this.ravitaillementSemestrePrecedentDinars = ravitaillementSemestrePrecedentDinars;
    }

    public Double getMontantRestantAgilisFinSemestre() {
        return montantRestantAgilisFinSemestre;
    }

    public void setMontantRestantAgilisFinSemestre(Double montantRestantAgilisFinSemestre) {
        this.montantRestantAgilisFinSemestre = montantRestantAgilisFinSemestre;
    }

    public Double getIndexFinSemestre() {
        return indexFinSemestre;
    }

    public void setIndexFinSemestre(Double indexFinSemestre) {
        this.indexFinSemestre = indexFinSemestre;
    }

    public Double getTotalRavitaillementLitres() {
        return totalRavitaillementLitres;
    }

    public void setTotalRavitaillementLitres(Double totalRavitaillementLitres) {
        this.totalRavitaillementLitres = totalRavitaillementLitres;
    }

    public Double getQuantiteRestanteReservoirAgilis() {
        return quantiteRestanteReservoirAgilis;
    }

    public void setQuantiteRestanteReservoirAgilis(Double quantiteRestanteReservoirAgilis) {
        this.quantiteRestanteReservoirAgilis = quantiteRestanteReservoirAgilis;
    }

    public Double getNbHeuresTravail() {
        return nbHeuresTravail;
    }

    public void setNbHeuresTravail(Double nbHeuresTravail) {
        this.nbHeuresTravail = nbHeuresTravail;
    }

    public Double getPourcentageConsommation() {
        return pourcentageConsommation;
    }

    public void setPourcentageConsommation(Double pourcentageConsommation) {
        this.pourcentageConsommation = pourcentageConsommation;
    }

    public Double getCarburantDemandeDinarsCours() {
        return carburantDemandeDinarsCours;
    }

    public void setCarburantDemandeDinarsCours(Double carburantDemandeDinarsCours) {
        this.carburantDemandeDinarsCours = carburantDemandeDinarsCours;
    }

    public String getEvaluationTauxConsommation() {
        return evaluationTauxConsommation;
    }

    public void setEvaluationTauxConsommation(String evaluationTauxConsommation) {
        this.evaluationTauxConsommation = evaluationTauxConsommation;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}