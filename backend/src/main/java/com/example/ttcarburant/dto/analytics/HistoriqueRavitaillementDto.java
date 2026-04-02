// ── HistoriqueRavitaillementDto.java ──────────────────────────────────────────
// package com.example.ttcarburant.dto.analytics;

package com.example.ttcarburant.dto.analytics;

import java.time.LocalDateTime;

public class HistoriqueRavitaillementDto {
    private Long id;
    private String vehiculeMatricule;
    private String vehiculeMarqueModele;
    private String vehiculeZoneNom;
    private int annee;
    private int mois;
    private String periodeLabel;          // "Janvier 2026"

    // Saisies
    private double indexDemarrageMois;
    private double indexFinMois;
    private double montantRestantMoisPrecedent;
    private double ravitaillementMoisPrecedent;
    private double ravitaillementMois;

    // Calculés
    private double totalRavitaillementLitres;
    private double quantiteRestanteReservoir;
    private double distanceParcourue;
    private double pourcentageConsommation;
    private double carburantDemandeDinars;
    private double montantRestantReservoirFin;

    // Budget
    private double coutDuMois;
    private double prixCarburant;
    private boolean budgetDepasse;
    private double depassementMontant;
    private double tauxBudget;           // % du budget consommé

    // Métadonnées
    private LocalDateTime dateCreation;
    private String statut;               // "NORMAL", "ALERTE_BUDGET", "ANOMALIE_CONSO", "ANOMALIE_KM"

    public HistoriqueRavitaillementDto() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVehiculeMatricule() { return vehiculeMatricule; }
    public void setVehiculeMatricule(String v) { this.vehiculeMatricule = v; }
    public String getVehiculeMarqueModele() { return vehiculeMarqueModele; }
    public void setVehiculeMarqueModele(String v) { this.vehiculeMarqueModele = v; }
    public String getVehiculeZoneNom() { return vehiculeZoneNom; }
    public void setVehiculeZoneNom(String v) { this.vehiculeZoneNom = v; }
    public int getAnnee() { return annee; }
    public void setAnnee(int v) { this.annee = v; }
    public int getMois() { return mois; }
    public void setMois(int v) { this.mois = v; }
    public String getPeriodeLabel() { return periodeLabel; }
    public void setPeriodeLabel(String v) { this.periodeLabel = v; }
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
    public double getCoutDuMois() { return coutDuMois; }
    public void setCoutDuMois(double v) { this.coutDuMois = v; }
    public double getPrixCarburant() { return prixCarburant; }
    public void setPrixCarburant(double v) { this.prixCarburant = v; }
    public boolean isBudgetDepasse() { return budgetDepasse; }
    public void setBudgetDepasse(boolean v) { this.budgetDepasse = v; }
    public double getDepassementMontant() { return depassementMontant; }
    public void setDepassementMontant(double v) { this.depassementMontant = v; }
    public double getTauxBudget() { return tauxBudget; }
    public void setTauxBudget(double v) { this.tauxBudget = v; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime v) { this.dateCreation = v; }
    public String getStatut() { return statut; }
    public void setStatut(String v) { this.statut = v; }
}