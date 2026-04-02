package com.example.ttcarburant.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité de verrouillage mensuel d'une période de saisie carburant.
 * Une fois verrouillé, aucune modification/suppression n'est possible
 * sauf si l'admin déverrouille explicitement.
 */
@Entity
@Table(name = "verrouillage_carburant",
        uniqueConstraints = @UniqueConstraint(columnNames = {"annee", "mois", "zone_id"}))
public class VerrouillageCarburant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int annee;

    @Column(nullable = false)
    private int mois;

    /** null = verrouillage global (toutes zones), non-null = zone spécifique */
    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(nullable = false)
    private boolean verrouille = false;

    @Column(name = "verrouille_par")
    private String verrouilleParEmail;

    @Column(name = "verrouille_le")
    private LocalDateTime verrouillerLe;

    @Column(name = "deverrouille_par")
    private String deverrouilleParEmail;

    @Column(name = "deverrouille_le")
    private LocalDateTime deverrouillerLe;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }

    // ── Getters & Setters ──
    public Long getId() { return id; }
    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }
    public int getMois() { return mois; }
    public void setMois(int mois) { this.mois = mois; }
    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }
    public boolean isVerrouille() { return verrouille; }
    public void setVerrouille(boolean verrouille) { this.verrouille = verrouille; }
    public String getVerrouilleParEmail() { return verrouilleParEmail; }
    public void setVerrouilleParEmail(String v) { this.verrouilleParEmail = v; }
    public LocalDateTime getVerrouillerLe() { return verrouillerLe; }
    public void setVerrouillerLe(LocalDateTime v) { this.verrouillerLe = v; }
    public String getDeverrouilleParEmail() { return deverrouilleParEmail; }
    public void setDeverrouilleParEmail(String v) { this.deverrouilleParEmail = v; }
    public LocalDateTime getDeverrouillerLe() { return deverrouillerLe; }
    public void setDeverrouillerLe(LocalDateTime v) { this.deverrouillerLe = v; }
    public LocalDateTime getDateCreation() { return dateCreation; }
}