package com.example.ttcarburant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ZoneRequest {

    @NotBlank(message = "Le nom de la zone est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String nom;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;

    @Size(max = 100, message = "Le nom du responsable ne peut pas dépasser 100 caractères")
    private String responsable;

    // Constructeurs
    public ZoneRequest() {}

    public ZoneRequest(String nom, String description, String responsable) {
        this.nom = nom;
        this.description = description;
        this.responsable = responsable;
    }

    // Getters et Setters
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }
}