package com.example.ttcarburant.model.entity;

import com.example.ttcarburant.model.enums.TypeCarburant;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "vehicules")
public class Vehicule {

    @Id
    @Column(nullable = false, unique = true, length = 20)
    private String matricule;

    @Column(nullable = false)
    private LocalDate dateMiseService;

    @Column(nullable = false, length = 50)
    private String marqueModele;

    @Column(nullable = false, length = 50)
    private String typeVehicule;

    @Column(length = 50)
    private String subdivision;

    @Column(length = 50)
    private String centre; // Central/CSC/ROC/Unité

    @Column(length = 100)
    private String residenceService;

    @Column(length = 100)
    private String nomConducteur;

    @Column(length = 100)
    private String prenomConducteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCarburant typeCarburant;

    @Column(nullable = false)
    private double prixCarburant;

    private double indexVidange;
    private LocalDate visiteTechnique;
    private double indexPneumatique;

    // Cumul annuel
    private double kilometrageTotal;
    private double consommationDinarsCumul;
    private double consommationLitresCumul;

    private double coutDuMois; // coût du mois (budget)
    private double croxChaine;
    private double indexBatterie;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @PrePersist
    protected void onCreate() {
        if (dateMiseService == null) dateMiseService = LocalDate.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public LocalDate getDateMiseService() { return dateMiseService; }
    public void setDateMiseService(LocalDate dateMiseService) { this.dateMiseService = dateMiseService; }

    public String getMarqueModele() { return marqueModele; }
    public void setMarqueModele(String marqueModele) { this.marqueModele = marqueModele; }

    public String getTypeVehicule() { return typeVehicule; }
    public void setTypeVehicule(String typeVehicule) { this.typeVehicule = typeVehicule; }

    public String getSubdivision() { return subdivision; }
    public void setSubdivision(String subdivision) { this.subdivision = subdivision; }

    public String getCentre() { return centre; }
    public void setCentre(String centre) { this.centre = centre; }

    public String getResidenceService() { return residenceService; }
    public void setResidenceService(String residenceService) { this.residenceService = residenceService; }

    public String getNomConducteur() { return nomConducteur; }
    public void setNomConducteur(String nomConducteur) { this.nomConducteur = nomConducteur; }

    public String getPrenomConducteur() { return prenomConducteur; }
    public void setPrenomConducteur(String prenomConducteur) { this.prenomConducteur = prenomConducteur; }

    public TypeCarburant getTypeCarburant() { return typeCarburant; }
    public void setTypeCarburant(TypeCarburant typeCarburant) { this.typeCarburant = typeCarburant; }

    public double getPrixCarburant() { return prixCarburant; }
    public void setPrixCarburant(double prixCarburant) { this.prixCarburant = prixCarburant; }

    public double getIndexVidange() { return indexVidange; }
    public void setIndexVidange(double indexVidange) { this.indexVidange = indexVidange; }

    public LocalDate getVisiteTechnique() { return visiteTechnique; }
    public void setVisiteTechnique(LocalDate visiteTechnique) { this.visiteTechnique = visiteTechnique; }

    public double getIndexPneumatique() { return indexPneumatique; }
    public void setIndexPneumatique(double indexPneumatique) { this.indexPneumatique = indexPneumatique; }

    public double getKilometrageTotal() { return kilometrageTotal; }
    public void setKilometrageTotal(double kilometrageTotal) { this.kilometrageTotal = kilometrageTotal; }

    public double getConsommationDinarsCumul() { return consommationDinarsCumul; }
    public void setConsommationDinarsCumul(double consommationDinarsCumul) { this.consommationDinarsCumul = consommationDinarsCumul; }

    public double getConsommationLitresCumul() { return consommationLitresCumul; }
    public void setConsommationLitresCumul(double consommationLitresCumul) { this.consommationLitresCumul = consommationLitresCumul; }

    public double getCoutDuMois() { return coutDuMois; }
    public void setCoutDuMois(double coutDuMois) { this.coutDuMois = coutDuMois; }

    public double getCroxChaine() { return croxChaine; }
    public void setCroxChaine(double croxChaine) { this.croxChaine = croxChaine; }

    public double getIndexBatterie() { return indexBatterie; }
    public void setIndexBatterie(double indexBatterie) { this.indexBatterie = indexBatterie; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }
}