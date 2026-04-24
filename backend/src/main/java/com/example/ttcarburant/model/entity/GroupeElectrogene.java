// com.example.ttcarburant.model.entity.GroupeElectrogene.java
package com.example.ttcarburant.model.entity;

import com.example.ttcarburant.model.enums.TypeCarburant;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "groupes_electrogenes")
public class GroupeElectrogene {

    @Id
    @Column(nullable = false, unique = true, length = 100)
    private String site;          // ex: "HACHED 1", "KASBAH 1"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCarburant typeCarburant;

    private Double puissanceKVA;          // peut être "démonté" -> null
    private Double tauxConsommationParHeure;
    private Double consommationTotaleMaxParSemestre;

    private Double prixCarburant;         // prix unitaire en DT/L

    // Informations carte Agilis
    private String typeCarte;
    private String numeroCarte;
    private LocalDate dateExpiration;
    private String codePIN;
    private String codePUK;
    private String utilisateurRoc;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    // Constructeurs, getters, setters...
    public GroupeElectrogene() {}

    // Getters et setters générés manuellement ou via Lombok
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public TypeCarburant getTypeCarburant() { return typeCarburant; }
    public void setTypeCarburant(TypeCarburant typeCarburant) { this.typeCarburant = typeCarburant; }
    public Double getPuissanceKVA() { return puissanceKVA; }
    public void setPuissanceKVA(Double puissanceKVA) { this.puissanceKVA = puissanceKVA; }
    public Double getTauxConsommationParHeure() { return tauxConsommationParHeure; }
    public void setTauxConsommationParHeure(Double tauxConsommationParHeure) { this.tauxConsommationParHeure = tauxConsommationParHeure; }
    public Double getConsommationTotaleMaxParSemestre() { return consommationTotaleMaxParSemestre; }
    public void setConsommationTotaleMaxParSemestre(Double consommationTotaleMaxParSemestre) { this.consommationTotaleMaxParSemestre = consommationTotaleMaxParSemestre; }
    public Double getPrixCarburant() { return prixCarburant; }
    public void setPrixCarburant(Double prixCarburant) { this.prixCarburant = prixCarburant; }
    public String getTypeCarte() { return typeCarte; }
    public void setTypeCarte(String typeCarte) { this.typeCarte = typeCarte; }
    public String getNumeroCarte() { return numeroCarte; }
    public void setNumeroCarte(String numeroCarte) { this.numeroCarte = numeroCarte; }
    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }
    public String getCodePIN() { return codePIN; }
    public void setCodePIN(String codePIN) { this.codePIN = codePIN; }
    public String getCodePUK() { return codePUK; }
    public void setCodePUK(String codePUK) { this.codePUK = codePUK; }
    public String getUtilisateurRoc() { return utilisateurRoc; }
    public void setUtilisateurRoc(String utilisateurRoc) { this.utilisateurRoc = utilisateurRoc; }
    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }
}