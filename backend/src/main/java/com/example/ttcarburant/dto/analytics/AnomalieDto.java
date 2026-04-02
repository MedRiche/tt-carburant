package com.example.ttcarburant.dto.analytics;

public class AnomalieDto {
    private String vehiculeMatricule;
    private String vehiculeMarqueModele;
    private String vehiculeZoneNom;
    private int annee;
    private int mois;
    private String periodeLabel;

    // Type anomalie: BUDGET_DEPASSE | CONSO_ANORMALE | KM_INCOHERENT | CARBURANT_ELEVE
    private String typeAnomalie;
    private String severite;      // FAIBLE | MOYENNE | ELEVEE | CRITIQUE
    private String description;
    private double valeurReelle;
    private double valeurSeuil;
    private double ecart;         // valeurReelle - valeurSeuil
    private double ecartPourcentage;

    public AnomalieDto() {}
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
    public String getTypeAnomalie() { return typeAnomalie; }
    public void setTypeAnomalie(String v) { this.typeAnomalie = v; }
    public String getSeverite() { return severite; }
    public void setSeverite(String v) { this.severite = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public double getValeurReelle() { return valeurReelle; }
    public void setValeurReelle(double v) { this.valeurReelle = v; }
    public double getValeurSeuil() { return valeurSeuil; }
    public void setValeurSeuil(double v) { this.valeurSeuil = v; }
    public double getEcart() { return ecart; }
    public void setEcart(double v) { this.ecart = v; }
    public double getEcartPourcentage() { return ecartPourcentage; }
    public void setEcartPourcentage(double v) { this.ecartPourcentage = v; }
}