package com.example.ttcarburant.dto.Maintenance;

import com.example.ttcarburant.model.enums.StatutMaintenance;
import com.example.ttcarburant.model.enums.TypeIntervention;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class MaintenanceRequest {

    @NotBlank
    private String vehiculeMatricule;

    @NotBlank
    private String numeroDossier;

    private LocalDate dateIntervention;

    @NotNull
    private TypeIntervention typeIntervention;

    private StatutMaintenance statut;

    private String description;

    private List<DetailMaintenanceDto> details;

    // Constructeur par défaut
    public MaintenanceRequest() {
    }

    // Getters et Setters
    public String getVehiculeMatricule() {
        return vehiculeMatricule;
    }

    public void setVehiculeMatricule(String vehiculeMatricule) {
        this.vehiculeMatricule = vehiculeMatricule;
    }

    public String getNumeroDossier() {
        return numeroDossier;
    }

    public void setNumeroDossier(String numeroDossier) {
        this.numeroDossier = numeroDossier;
    }

    public LocalDate getDateIntervention() {
        return dateIntervention;
    }

    public void setDateIntervention(LocalDate dateIntervention) {
        this.dateIntervention = dateIntervention;
    }

    public TypeIntervention getTypeIntervention() {
        return typeIntervention;
    }

    public void setTypeIntervention(TypeIntervention typeIntervention) {
        this.typeIntervention = typeIntervention;
    }

    public StatutMaintenance getStatut() {
        return statut;
    }

    public void setStatut(StatutMaintenance statut) {
        this.statut = statut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DetailMaintenanceDto> getDetails() {
        return details;
    }

    public void setDetails(List<DetailMaintenanceDto> details) {
        this.details = details;
    }
}