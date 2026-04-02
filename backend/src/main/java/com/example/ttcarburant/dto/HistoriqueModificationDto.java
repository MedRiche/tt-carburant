package com.example.ttcarburant.dto;

import java.time.LocalDateTime;

public class HistoriqueModificationDto {

    private Long id;
    private Long gestionId;
    private String vehiculeMatricule;
    private String vehiculeMarqueModele;
    private int annee;
    private int mois;
    private String periodeLabel;
    private String action;       // CREATE / UPDATE / DELETE
    private String actionLabel;  // Créé / Modifié / Supprimé
    private String modifiePar;
    private LocalDateTime modifieLe;
    private String valeursAvant;
    private String valeursApres;
    private String description;

    public HistoriqueModificationDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGestionId() { return gestionId; }
    public void setGestionId(Long v) { this.gestionId = v; }
    public String getVehiculeMatricule() { return vehiculeMatricule; }
    public void setVehiculeMatricule(String v) { this.vehiculeMatricule = v; }
    public String getVehiculeMarqueModele() { return vehiculeMarqueModele; }
    public void setVehiculeMarqueModele(String v) { this.vehiculeMarqueModele = v; }
    public int getAnnee() { return annee; }
    public void setAnnee(int v) { this.annee = v; }
    public int getMois() { return mois; }
    public void setMois(int v) { this.mois = v; }
    public String getPeriodeLabel() { return periodeLabel; }
    public void setPeriodeLabel(String v) { this.periodeLabel = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String v) { this.actionLabel = v; }
    public String getModifiePar() { return modifiePar; }
    public void setModifiePar(String v) { this.modifiePar = v; }
    public LocalDateTime getModifieLe() { return modifieLe; }
    public void setModifieLe(LocalDateTime v) { this.modifieLe = v; }
    public String getValeursAvant() { return valeursAvant; }
    public void setValeursAvant(String v) { this.valeursAvant = v; }
    public String getValeursApres() { return valeursApres; }
    public void setValeursApres(String v) { this.valeursApres = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}