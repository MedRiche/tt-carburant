package com.example.ttcarburant.dto;

public class CarburantVehiculeDto {
    private Long id;
    private String vehiculeMatricule;
    private String vehiculeMarqueModele;
    private String vehiculeZoneNom;
    private double prixCarburant;
    private double coutDuMois;
    private int annee;
    private int mois;

    // Saisies
    private double indexDemarrageMois;
    private double indexFinMois;
    private double montantRestantMoisPrecedent;
    private double ravitaillementMoisPrecedent;
    private double ravitaillementMois;

    // Calculés DAF 2026
    private double totalRavitaillementLitres;
    private double quantiteRestanteReservoir;
    private double distanceParcourue;
    private double pourcentageConsommation;
    private double carburantDemandeDinars;

    // NOUVEAU : montant restant fin de mois (pour pré-remplissage règle 7)
    private double montantRestantReservoirFin;

    // NOUVEAU : alertes budget
    private boolean budgetDepasse;
    private double depassementMontant;

    public CarburantVehiculeDto() {}

    // ── Getters & Setters ──
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVehiculeMatricule() { return vehiculeMatricule; }
    public void setVehiculeMatricule(String v) { this.vehiculeMatricule = v; }
    public String getVehiculeMarqueModele() { return vehiculeMarqueModele; }
    public void setVehiculeMarqueModele(String v) { this.vehiculeMarqueModele = v; }
    public String getVehiculeZoneNom() { return vehiculeZoneNom; }
    public void setVehiculeZoneNom(String v) { this.vehiculeZoneNom = v; }
    public double getPrixCarburant() { return prixCarburant; }
    public void setPrixCarburant(double v) { this.prixCarburant = v; }
    public double getCoutDuMois() { return coutDuMois; }
    public void setCoutDuMois(double v) { this.coutDuMois = v; }
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
    public double getTotalRavitaillementLitres() { return totalRavitaillementLitres; }
    public void setTotalRavitaillementLitres(double v) { this.totalRavitaillementLitres = v; }
    public double getQuantiteRestanteReservoir() { return quantiteRestanteReservoir; }
    public void setQuantiteRestanteReservoir(double v) { this.quantiteRestanteReservoir = v; }
    public double getDistanceParcourue() { return distanceParcourue; }
    public void setDistanceParcourue(double v) { this.distanceParcourue = v; }
    public double getPourcentageConsommation() { return pourcentageConsommation; }
    public void setPourcentageConsommation(double v) { this.pourcentageConsommation = v; }
    public double getCarburantDemandeDinars() { return carburantDemandeDinars; }
    public void setCarburantDemandeDinars(double v) { this.carburantDemandeDinars = v; }
    public double getMontantRestantReservoirFin() { return montantRestantReservoirFin; }
    public void setMontantRestantReservoirFin(double v) { this.montantRestantReservoirFin = v; }
    public boolean isBudgetDepasse() { return budgetDepasse; }
    public void setBudgetDepasse(boolean v) { this.budgetDepasse = v; }
    public double getDepassementMontant() { return depassementMontant; }
    public void setDepassementMontant(double v) { this.depassementMontant = v; }
}