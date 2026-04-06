package com.example.ttcarburant.model.entity;

import com.example.ttcarburant.model.enums.StatutMaintenance;
import com.example.ttcarburant.model.enums.TypeIntervention;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "maintenances")
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_dossier", nullable = false, length = 50)
    private String numeroDossier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicule_matricule", nullable = false)
    private Vehicule vehicule;

    @Column(name = "date_intervention")
    private LocalDate dateIntervention;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_intervention", nullable = false)
    private TypeIntervention typeIntervention;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMaintenance statut = StatutMaintenance.EN_COURS;

    @Column(length = 1000)
    private String description;

    @Column(name = "cout_total_htva", columnDefinition = "double default 0")
    private double coutTotalHtva;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "cree_par")
    private String creePar;

    @OneToMany(mappedBy = "maintenance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetailMaintenance> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }

    // ── Getters & Setters ──
    public Long getId() { return id; }
    public String getNumeroDossier() { return numeroDossier; }
    public void setNumeroDossier(String v) { this.numeroDossier = v; }
    public Vehicule getVehicule() { return vehicule; }
    public void setVehicule(Vehicule v) { this.vehicule = v; }
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
    public LocalDateTime getDateCreation() { return dateCreation; }
    public String getCreePar() { return creePar; }
    public void setCreePar(String v) { this.creePar = v; }
    public List<DetailMaintenance> getDetails() { return details; }
    public void setDetails(List<DetailMaintenance> v) { this.details = v; }

    /** Recalcule le total HTVA depuis les détails */
    public void recalculerTotal() {
        this.coutTotalHtva = details.stream()
                .mapToDouble(DetailMaintenance::getTotalHtva)
                .sum();
        this.coutTotalHtva = Math.round(this.coutTotalHtva * 1000.0) / 1000.0;
    }
}