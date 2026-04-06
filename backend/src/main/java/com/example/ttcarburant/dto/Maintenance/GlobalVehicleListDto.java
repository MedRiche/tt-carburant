package com.example.ttcarburant.dto.Maintenance;

public class GlobalVehicleListDto {

    private String vehiculeId;
    private double totalHtva;
    private String brands;
    private String vehiculeMarque;
    private String zoneNom;
    private int nbDossiers;
    private int nbDetails;

    // Constructeur par défaut
    public GlobalVehicleListDto() {
    }

    // Getters et Setters
    public String getVehiculeId() {
        return vehiculeId;
    }

    public void setVehiculeId(String vehiculeId) {
        this.vehiculeId = vehiculeId;
    }

    public double getTotalHtva() {
        return totalHtva;
    }

    public void setTotalHtva(double totalHtva) {
        this.totalHtva = totalHtva;
    }

    public String getBrands() {
        return brands;
    }

    public void setBrands(String brands) {
        this.brands = brands;
    }

    public String getVehiculeMarque() {
        return vehiculeMarque;
    }

    public void setVehiculeMarque(String vehiculeMarque) {
        this.vehiculeMarque = vehiculeMarque;
    }

    public String getZoneNom() {
        return zoneNom;
    }

    public void setZoneNom(String zoneNom) {
        this.zoneNom = zoneNom;
    }

    public int getNbDossiers() {
        return nbDossiers;
    }

    public void setNbDossiers(int nbDossiers) {
        this.nbDossiers = nbDossiers;
    }

    public int getNbDetails() {
        return nbDetails;
    }

    public void setNbDetails(int nbDetails) {
        this.nbDetails = nbDetails;
    }
}