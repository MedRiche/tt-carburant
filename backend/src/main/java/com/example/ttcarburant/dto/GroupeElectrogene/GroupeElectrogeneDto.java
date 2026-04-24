package com.example.ttcarburant.dto.GroupeElectrogene;


import com.example.ttcarburant.model.enums.TypeCarburant;
import java.time.LocalDate;

public class GroupeElectrogeneDto {

    private String site;
    private TypeCarburant typeCarburant;
    private Double puissanceKVA;
    private Double tauxConsommationParHeure;
    private Double consommationTotaleMaxParSemestre;
    private Double prixCarburant;
    private String typeCarte;
    private String numeroCarte;
    private LocalDate dateExpiration;
    private String codePIN;
    private String codePUK;
    private String utilisateurRoc;
    private Long zoneId;
    private String zoneNom;

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public TypeCarburant getTypeCarburant() {
        return typeCarburant;
    }

    public void setTypeCarburant(TypeCarburant typeCarburant) {
        this.typeCarburant = typeCarburant;
    }

    public Double getPuissanceKVA() {
        return puissanceKVA;
    }

    public void setPuissanceKVA(Double puissanceKVA) {
        this.puissanceKVA = puissanceKVA;
    }

    public Double getTauxConsommationParHeure() {
        return tauxConsommationParHeure;
    }

    public void setTauxConsommationParHeure(Double tauxConsommationParHeure) {
        this.tauxConsommationParHeure = tauxConsommationParHeure;
    }

    public Double getConsommationTotaleMaxParSemestre() {
        return consommationTotaleMaxParSemestre;
    }

    public void setConsommationTotaleMaxParSemestre(Double consommationTotaleMaxParSemestre) {
        this.consommationTotaleMaxParSemestre = consommationTotaleMaxParSemestre;
    }

    public Double getPrixCarburant() {
        return prixCarburant;
    }

    public void setPrixCarburant(Double prixCarburant) {
        this.prixCarburant = prixCarburant;
    }

    public String getTypeCarte() {
        return typeCarte;
    }

    public void setTypeCarte(String typeCarte) {
        this.typeCarte = typeCarte;
    }

    public String getNumeroCarte() {
        return numeroCarte;
    }

    public void setNumeroCarte(String numeroCarte) {
        this.numeroCarte = numeroCarte;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public String getCodePIN() {
        return codePIN;
    }

    public void setCodePIN(String codePIN) {
        this.codePIN = codePIN;
    }

    public String getCodePUK() {
        return codePUK;
    }

    public void setCodePUK(String codePUK) {
        this.codePUK = codePUK;
    }

    public String getUtilisateurRoc() {
        return utilisateurRoc;
    }

    public void setUtilisateurRoc(String utilisateurRoc) {
        this.utilisateurRoc = utilisateurRoc;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneNom() {
        return zoneNom;
    }

    public void setZoneNom(String zoneNom) {
        this.zoneNom = zoneNom;
    }
}