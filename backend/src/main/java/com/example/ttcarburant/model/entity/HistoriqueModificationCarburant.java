package com.example.ttcarburant.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Historique des modifications de saisie carburant.
 * Chaque modification (CREATE / UPDATE / DELETE) est tracée.
 */
@Entity
@Table(name = "historique_modification_carburant")
public class HistoriqueModificationCarburant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Référence à l'enregistrement modifié (peut être null si supprimé) */
    @Column(name = "gestion_id")
    private Long gestionId;

    @Column(name = "vehicule_matricule", nullable = false)
    private String vehiculeMatricule;

    @Column(nullable = false)
    private int annee;

    @Column(nullable = false)
    private int mois;

    /** CREATE / UPDATE / DELETE */
    @Column(nullable = false, length = 10)
    private String action;

    /** Email de l'utilisateur qui a effectué l'action */
    @Column(name = "modifie_par", nullable = false)
    private String modifiePar;

    @Column(name = "modifie_le", nullable = false)
    private LocalDateTime modifieLe;

    /** Snapshot JSON des valeurs AVANT modification */
    @Column(name = "valeurs_avant", columnDefinition = "TEXT")
    private String valeursAvant;

    /** Snapshot JSON des valeurs APRÈS modification */
    @Column(name = "valeurs_apres", columnDefinition = "TEXT")
    private String valeursApres;

    /** Description lisible du changement */
    @Column(length = 500)
    private String description;

    @PrePersist
    protected void onCreate() {
        modifieLe = LocalDateTime.now();
    }

    // ── Getters & Setters ──
    public Long getId() { return id; }
    public Long getGestionId() { return gestionId; }
    public void setGestionId(Long v) { this.gestionId = v; }
    public String getVehiculeMatricule() { return vehiculeMatricule; }
    public void setVehiculeMatricule(String v) { this.vehiculeMatricule = v; }
    public int getAnnee() { return annee; }
    public void setAnnee(int v) { this.annee = v; }
    public int getMois() { return mois; }
    public void setMois(int v) { this.mois = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
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