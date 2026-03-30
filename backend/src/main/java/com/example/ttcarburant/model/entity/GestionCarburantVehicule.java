package com.example.ttcarburant.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gestion_carburant_vehicule",
        uniqueConstraints = @UniqueConstraint(columnNames = {"vehicule_matricule","annee","mois"}))
public class GestionCarburantVehicule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicule_matricule", nullable = false)
    private Vehicule vehicule;

    @Column(nullable = false) private int annee;
    @Column(nullable = false) private int mois;

    // ── Données saisies ──────────────────────────────
    private double indexDemarrageMois;
    private double indexFinMois;
    private double montantRestantMoisPrecedent;
    private double ravitaillementMoisPrecedent;
    private double ravitaillementMois;

    // ── Calculés DAF 2026 ────────────────────────────
    private double totalRavitaillementLitres;
    private double quantiteRestanteReservoir;
    private double distanceParcourue;
    private double pourcentageConsommation;
    private double carburantDemandeDinars;

    // ── NOUVEAU : montant restant fin de mois (pour règle 7 mois suivant) ──
    @Column(name = "montant_restant_reservoir_fin", columnDefinition = "double default 0")
    private double montantRestantReservoirFin;

    // ── NOUVEAU : alerte budget dépassé ──
    @Column(name = "budget_depasse", columnDefinition = "boolean default false")
    private boolean budgetDepasse;

    @Column(name = "depassement_montant", columnDefinition = "double default 0")
    private double depassementMontant;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist protected void onCreate() { dateCreation = LocalDateTime.now(); }

    // ── Getters / Setters ────────────────────────────
    public Long getId() { return id; }
    public Vehicule getVehicule() { return vehicule; }
    public void setVehicule(Vehicule v) { this.vehicule = v; }
    public int getAnnee() { return annee; }
    public void setAnnee(int a) { this.annee = a; }
    public int getMois() { return mois; }
    public void setMois(int m) { this.mois = m; }
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
    public LocalDateTime getDateCreation() { return dateCreation; }
}