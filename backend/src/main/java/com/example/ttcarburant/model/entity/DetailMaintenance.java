package com.example.ttcarburant.model.entity;

import com.example.ttcarburant.model.enums.TypeDetailMaintenance;
import jakarta.persistence.*;

@Entity
@Table(name = "detail_maintenance")
public class DetailMaintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "maintenance_id", nullable = false)
    private Maintenance maintenance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDetailMaintenance type; // MAIN_D_OEUVRE | PIECE

    // Identique à N° Dossier du parent (héritage dataset)
    @Column(name = "numero_dossier", length = 50)
    private String numeroDossier;

    @Column(length = 100)
    private String marque;

    // Numéro de prestation (main d'oeuvre) ou référence (pièce)
    @Column(length = 100)
    private String numero;

    @Column(name = "numero_piece", length = 100)
    private String numeroPiece;

    @Column(nullable = false, length = 500)
    private String designation;

    @Column(nullable = false)
    private int quantite = 1;

    @Column(name = "montant_unitaire", nullable = false)
    private double montantUnitaire;

    @Column(name = "total_htva")
    private double totalHtva;

    // ── Getters & Setters ──
    public Long getId() { return id; }
    public Maintenance getMaintenance() { return maintenance; }
    public void setMaintenance(Maintenance v) { this.maintenance = v; }
    public TypeDetailMaintenance getType() { return type; }
    public void setType(TypeDetailMaintenance v) { this.type = v; }
    public String getNumeroDossier() { return numeroDossier; }
    public void setNumeroDossier(String v) { this.numeroDossier = v; }
    public String getMarque() { return marque; }
    public void setMarque(String v) { this.marque = v; }
    public String getNumero() { return numero; }
    public void setNumero(String v) { this.numero = v; }
    public String getNumeroPiece() { return numeroPiece; }
    public void setNumeroPiece(String v) { this.numeroPiece = v; }
    public String getDesignation() { return designation; }
    public void setDesignation(String v) { this.designation = v; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int v) { this.quantite = v; }
    public double getMontantUnitaire() { return montantUnitaire; }
    public void setMontantUnitaire(double v) { this.montantUnitaire = v; }
    public double getTotalHtva() { return totalHtva; }
    public void setTotalHtva(double v) { this.totalHtva = v; }

    /** Calcule le total HTVA = quantité × montant unitaire */
    public void calculerMontant() {
        this.totalHtva = Math.round(this.quantite * this.montantUnitaire * 1000.0) / 1000.0;
    }
}