package com.example.ttcarburant.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "affectation_utilisateur_zone")
public class AffectationUtilisateurZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateAffectation;

    @ManyToOne
    @JoinColumn(name = "affecte_par_id")
    private Utilisateur affectePar;

    @PrePersist
    protected void onCreate() {
        dateAffectation = LocalDateTime.now();
    }

    // Constructeurs
    public AffectationUtilisateurZone() {}

    public AffectationUtilisateurZone(Utilisateur utilisateur, Zone zone, Utilisateur affectePar) {
        this.utilisateur = utilisateur;
        this.zone = zone;
        this.affectePar = affectePar;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public LocalDateTime getDateAffectation() {
        return dateAffectation;
    }

    public Utilisateur getAffectePar() {
        return affectePar;
    }

    public void setAffectePar(Utilisateur affectePar) {
        this.affectePar = affectePar;
    }
}