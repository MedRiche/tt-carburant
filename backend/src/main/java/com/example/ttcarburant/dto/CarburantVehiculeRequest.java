package com.example.ttcarburant.dto;

import jakarta.validation.constraints.*;

public class CarburantVehiculeRequest {

    @NotBlank(message = "Le matricule est obligatoire")
    private String vehiculeMatricule;

    @Min(value = 2020, message = "Année invalide")
    private int annee;

    @Min(1) @Max(12)
    private int mois;

    @PositiveOrZero private double indexDemarrageMois;
    @PositiveOrZero private double indexFinMois;
    @PositiveOrZero private double montantRestantMoisPrecedent;
    @PositiveOrZero private double ravitaillementMoisPrecedent;
    @PositiveOrZero private double ravitaillementMois;

    public CarburantVehiculeRequest() {}

    public String getVehiculeMatricule() { return vehiculeMatricule; }
    public void setVehiculeMatricule(String v) { this.vehiculeMatricule = v; }
    public int getAnnee() { return annee; }
    public void setAnnee(int v) { this.annee = v; }
    public int getMois() { return mois; }
    public void setMois(int v) { this.mois = v; }
    public double getIndexDemarrageMois() { return indexDemarrageMois; }
    public void setIndexDemarrageMois(double v) { this.indexDemarrageMois = v; }
    public double getIndexFinMois() { return indexFinMois; }
    public void setIndexFinMois(double v) { this.indexFinMois = v; }
    public double getMontantRestantMoisPrecedent() { return montantRestantMoisPrecedent; }
    public void setMontantRestantMoisPrecedent(double v) { this.montantRestantMoisPrecedent = v; }
    public double getRavitaillementMoisPrecedent() { return ravitaillementMoisPrecedent; }
    public void setRavitaillementMoisPrecedent(double v) { this.ravitaillementMoisPrecedent = v; }
    public double getRavitaillementMois() { return ravitaillementMois; }
    public void setRavitaillementMois(double v) { this.ravitaillementMois = v; }
}