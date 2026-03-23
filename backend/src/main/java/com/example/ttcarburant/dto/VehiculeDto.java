package com.example.ttcarburant.dto;

import com.example.ttcarburant.model.enums.TypeCarburant;
import java.time.LocalDate;

public class VehiculeDto {
    private String matricule;
    private LocalDate dateMiseService;
    private String marqueModele;
    private String typeVehicule;
    private String subdivision;
    private String centre;
    private String residenceService;
    private String nomConducteur;
    private String prenomConducteur;
    private TypeCarburant typeCarburant;
    private double prixCarburant;
    private double indexVidange;
    private LocalDate visiteTechnique;
    private double indexPneumatique;
    private double kilometrageTotal;
    private double consommationDinarsCumul;
    private double consommationLitresCumul;
    private double coutDuMois;
    private double croxChaine;
    private double indexBatterie;
    private Long zoneId;
    private String zoneNom;

    public VehiculeDto() {}

    // Getters & Setters
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
    public void setConsommationDinarsCumul(double v) { this.consommationDinarsCumul = v; }

    public double getConsommationLitresCumul() { return consommationLitresCumul; }
    public void setConsommationLitresCumul(double v) { this.consommationLitresCumul = v; }

    public double getCoutDuMois() { return coutDuMois; }
    public void setCoutDuMois(double coutDuMois) { this.coutDuMois = coutDuMois; }

    public double getCroxChaine() { return croxChaine; }
    public void setCroxChaine(double croxChaine) { this.croxChaine = croxChaine; }

    public double getIndexBatterie() { return indexBatterie; }
    public void setIndexBatterie(double indexBatterie) { this.indexBatterie = indexBatterie; }

    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }

    public String getZoneNom() { return zoneNom; }
    public void setZoneNom(String zoneNom) { this.zoneNom = zoneNom; }
}