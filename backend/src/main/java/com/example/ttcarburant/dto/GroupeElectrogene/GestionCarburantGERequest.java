package com.example.ttcarburant.dto.GroupeElectrogene;


import com.example.ttcarburant.model.enums.Semestre;
import jakarta.validation.constraints.NotNull;

public class GestionCarburantGERequest {

    @NotNull
    private String site;

    @NotNull
    private int annee;

    @NotNull
    private Semestre semestre;

    private Double indexHeureSemestrePrecedent;
    private Double montantCarburantRestantReservoirPrecedent;
    private Double ravitaillementSemestrePrecedentDinars;
    private Double montantRestantAgilisFinSemestre;
    private Double indexFinSemestre;

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
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
}