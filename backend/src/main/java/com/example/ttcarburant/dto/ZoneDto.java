package com.example.ttcarburant.dto;

import java.time.LocalDateTime;

public class ZoneDto {

    private Long id;
    private String nom;
    private String description;
    private String responsable;
    private LocalDateTime dateCreation;
    private int nombreUtilisateurs;

    // Constructeurs
    public ZoneDto() {}

    public ZoneDto(Long id, String nom, String description, String responsable,
                   LocalDateTime dateCreation, int nombreUtilisateurs) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.responsable = responsable;
        this.dateCreation = dateCreation;
        this.nombreUtilisateurs = nombreUtilisateurs;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getNombreUtilisateurs() {
        return nombreUtilisateurs;
    }

    public void setNombreUtilisateurs(int nombreUtilisateurs) {
        this.nombreUtilisateurs = nombreUtilisateurs;
    }
}