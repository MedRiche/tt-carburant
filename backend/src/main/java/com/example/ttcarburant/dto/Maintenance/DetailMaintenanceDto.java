package com.example.ttcarburant.dto.Maintenance;

import com.example.ttcarburant.model.enums.TypeDetailMaintenance;

public class DetailMaintenanceDto {

    private Long id;
    private TypeDetailMaintenance type;
    private String numeroDossier;
    private String marque;
    private String numero;
    private String numeroPiece;
    private String designation;
    private int quantite;
    private double montantUnitaire;
    private double totalHtva;

    // Constructeur par défaut
    public DetailMaintenanceDto() {
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TypeDetailMaintenance getType() {
        return type;
    }

    public void setType(TypeDetailMaintenance type) {
        this.type = type;
    }

    public String getNumeroDossier() {
        return numeroDossier;
    }

    public void setNumeroDossier(String numeroDossier) {
        this.numeroDossier = numeroDossier;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getNumeroPiece() {
        return numeroPiece;
    }

    public void setNumeroPiece(String numeroPiece) {
        this.numeroPiece = numeroPiece;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public double getMontantUnitaire() {
        return montantUnitaire;
    }

    public void setMontantUnitaire(double montantUnitaire) {
        this.montantUnitaire = montantUnitaire;
    }

    public double getTotalHtva() {
        return totalHtva;
    }

    public void setTotalHtva(double totalHtva) {
        this.totalHtva = totalHtva;
    }
}