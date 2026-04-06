// ═══════════════════════════════════════════════════════════════
// MaintenanceDto.java
// ═══════════════════════════════════════════════════════════════
package com.example.ttcarburant.dto.Maintenance;

import com.example.ttcarburant.model.enums.StatutMaintenance;
import com.example.ttcarburant.model.enums.TypeIntervention;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MaintenanceDto {
    private Long id;
    private String numeroDossier;
    private String vehiculeMatricule;
    private String vehiculeMarqueModele;
    private String vehiculeZoneNom;
    private LocalDate dateIntervention;
    private TypeIntervention typeIntervention;
    private StatutMaintenance statut;
    private String description;
    private double coutTotalHtva;
    private String creePar;
    private LocalDateTime dateCreation;
    private List<DetailMaintenanceDto> details;
    // Brands (prestataires distincts)
    private String brands;
    private int nbDetails;

    public MaintenanceDto() {}
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getNumeroDossier() { return numeroDossier; }
    public void setNumeroDossier(String v) { this.numeroDossier = v; }
    public String getVehiculeMatricule() { return vehiculeMatricule; }
    public void setVehiculeMatricule(String v) { this.vehiculeMatricule = v; }
    public String getVehiculeMarqueModele() { return vehiculeMarqueModele; }
    public void setVehiculeMarqueModele(String v) { this.vehiculeMarqueModele = v; }
    public String getVehiculeZoneNom() { return vehiculeZoneNom; }
    public void setVehiculeZoneNom(String v) { this.vehiculeZoneNom = v; }
    public LocalDate getDateIntervention() { return dateIntervention; }
    public void setDateIntervention(LocalDate v) { this.dateIntervention = v; }
    public TypeIntervention getTypeIntervention() { return typeIntervention; }
    public void setTypeIntervention(TypeIntervention v) { this.typeIntervention = v; }
    public StatutMaintenance getStatut() { return statut; }
    public void setStatut(StatutMaintenance v) { this.statut = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public double getCoutTotalHtva() { return coutTotalHtva; }
    public void setCoutTotalHtva(double v) { this.coutTotalHtva = v; }
    public String getCreePar() { return creePar; }
    public void setCreePar(String v) { this.creePar = v; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime v) { this.dateCreation = v; }
    public List<DetailMaintenanceDto> getDetails() { return details; }
    public void setDetails(List<DetailMaintenanceDto> v) { this.details = v; }
    public String getBrands() { return brands; }
    public void setBrands(String v) { this.brands = v; }
    public int getNbDetails() { return nbDetails; }
    public void setNbDetails(int v) { this.nbDetails = v; }
}