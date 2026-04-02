// ── EvolutionDto.java ─────────────────────────────────────────────────────────
// package com.example.ttcarburant.dto.analytics;

package com.example.ttcarburant.dto.analytics;

import java.util.List;

public class EvolutionDto {
    private String titre;
    private int annee;
    private List<String> labels;            // ["Jan","Fév","Mars"...]
    private List<Double> consommationLitres;
    private List<Double> coutDinars;
    private List<Double> kmParcourus;
    private List<Double> pourcentageConso;
    private List<Double> budgetMensuel;     // ligne de référence budget
    private double totalKm;
    private double totalLitres;
    private double totalCout;
    private double moyenneConsommation;     // L/100km

    public EvolutionDto() {}
    public String getTitre() { return titre; }
    public void setTitre(String v) { this.titre = v; }
    public int getAnnee() { return annee; }
    public void setAnnee(int v) { this.annee = v; }
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> v) { this.labels = v; }
    public List<Double> getConsommationLitres() { return consommationLitres; }
    public void setConsommationLitres(List<Double> v) { this.consommationLitres = v; }
    public List<Double> getCoutDinars() { return coutDinars; }
    public void setCoutDinars(List<Double> v) { this.coutDinars = v; }
    public List<Double> getKmParcourus() { return kmParcourus; }
    public void setKmParcourus(List<Double> v) { this.kmParcourus = v; }
    public List<Double> getPourcentageConso() { return pourcentageConso; }
    public void setPourcentageConso(List<Double> v) { this.pourcentageConso = v; }
    public List<Double> getBudgetMensuel() { return budgetMensuel; }
    public void setBudgetMensuel(List<Double> v) { this.budgetMensuel = v; }
    public double getTotalKm() { return totalKm; }
    public void setTotalKm(double v) { this.totalKm = v; }
    public double getTotalLitres() { return totalLitres; }
    public void setTotalLitres(double v) { this.totalLitres = v; }
    public double getTotalCout() { return totalCout; }
    public void setTotalCout(double v) { this.totalCout = v; }
    public double getMoyenneConsommation() { return moyenneConsommation; }
    public void setMoyenneConsommation(double v) { this.moyenneConsommation = v; }
}