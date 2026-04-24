// com.example.ttcarburant.model.entity.GestionCarburantGE.java
package com.example.ttcarburant.model.entity;

import com.example.ttcarburant.model.enums.Semestre;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gestion_carburant_ge",
        uniqueConstraints = @UniqueConstraint(columnNames = {"groupe_electrogene_site", "annee", "semestre"}))
public class GestionCarburantGE {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "groupe_electrogene_site", nullable = false)
    private GroupeElectrogene groupeElectrogene;

    @Column(nullable = false)
    private int annee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Semestre semestre;          // PREMIER ou DEUXIEME

    // Saisies
    private Double indexHeureSemestrePrecedent;
    private Double montantCarburantRestantReservoirPrecedent;
    private Double ravitaillementSemestrePrecedentDinars;
    private Double montantRestantAgilisFinSemestre;  // Montant restante dans le réservoir + cate agilis fin du semestre
    private Double indexFinSemestre;

    // Calculés (formules DAF)
    private Double totalRavitaillementLitres;
    private Double quantiteRestanteReservoirAgilis;
    private Double nbHeuresTravail;
    private Double pourcentageConsommation;
    private Double carburantDemandeDinarsCours;
    private String evaluationTauxConsommation;   // "OUI" ou "NON"

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() { dateCreation = LocalDateTime.now(); }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GroupeElectrogene getGroupeElectrogene() { return groupeElectrogene; }
    public void setGroupeElectrogene(GroupeElectrogene ge) { this.groupeElectrogene = ge; }
    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }
    public Semestre getSemestre() { return semestre; }
    public void setSemestre(Semestre semestre) { this.semestre = semestre; }
    public Double getIndexHeureSemestrePrecedent() { return indexHeureSemestrePrecedent; }
    public void setIndexHeureSemestrePrecedent(Double v) { this.indexHeureSemestrePrecedent = v; }
    public Double getMontantCarburantRestantReservoirPrecedent() { return montantCarburantRestantReservoirPrecedent; }
    public void setMontantCarburantRestantReservoirPrecedent(Double v) { this.montantCarburantRestantReservoirPrecedent = v; }
    public Double getRavitaillementSemestrePrecedentDinars() { return ravitaillementSemestrePrecedentDinars; }
    public void setRavitaillementSemestrePrecedentDinars(Double v) { this.ravitaillementSemestrePrecedentDinars = v; }
    public Double getMontantRestantAgilisFinSemestre() { return montantRestantAgilisFinSemestre; }
    public void setMontantRestantAgilisFinSemestre(Double v) { this.montantRestantAgilisFinSemestre = v; }
    public Double getIndexFinSemestre() { return indexFinSemestre; }
    public void setIndexFinSemestre(Double v) { this.indexFinSemestre = v; }
    public Double getTotalRavitaillementLitres() { return totalRavitaillementLitres; }
    public void setTotalRavitaillementLitres(Double v) { this.totalRavitaillementLitres = v; }
    public Double getQuantiteRestanteReservoirAgilis() { return quantiteRestanteReservoirAgilis; }
    public void setQuantiteRestanteReservoirAgilis(Double v) { this.quantiteRestanteReservoirAgilis = v; }
    public Double getNbHeuresTravail() { return nbHeuresTravail; }
    public void setNbHeuresTravail(Double v) { this.nbHeuresTravail = v; }
    public Double getPourcentageConsommation() { return pourcentageConsommation; }
    public void setPourcentageConsommation(Double v) { this.pourcentageConsommation = v; }
    public Double getCarburantDemandeDinarsCours() { return carburantDemandeDinarsCours; }
    public void setCarburantDemandeDinarsCours(Double v) { this.carburantDemandeDinarsCours = v; }
    public String getEvaluationTauxConsommation() { return evaluationTauxConsommation; }
    public void setEvaluationTauxConsommation(String v) { this.evaluationTauxConsommation = v; }
    public LocalDateTime getDateCreation() { return dateCreation; }
}