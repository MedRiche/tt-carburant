package com.example.ttcarburant.dto.analytics;

import java.util.List;
import java.util.Map;

public class DashboardCarburantDto {

    // KPI globaux
    private int nbVehiculesSaisis;
    private double totalKm;
    private double totalLitres;
    private double totalCoutDT;
    private double totalBudgetDT;
    private double tauxBudgetGlobal;     // totalCoutDT / totalBudgetDT * 100
    private int nbBudgetsDepasses;
    private int nbAnomalies;

    // Consommation par zone (pour bar chart / heatmap)
    public static class ZoneStat {
        private Long zoneId;
        private String zoneNom;
        private int nbVehicules;
        private double totalKm;
        private double totalLitres;
        private double totalCout;
        private double totalBudget;
        private double tauxBudget;
        private int nbAnomalies;
        private double rendementMoyen;   // L/100km

        public ZoneStat() {}
        public Long getZoneId() { return zoneId; }
        public void setZoneId(Long v) { this.zoneId = v; }
        public String getZoneNom() { return zoneNom; }
        public void setZoneNom(String v) { this.zoneNom = v; }
        public int getNbVehicules() { return nbVehicules; }
        public void setNbVehicules(int v) { this.nbVehicules = v; }
        public double getTotalKm() { return totalKm; }
        public void setTotalKm(double v) { this.totalKm = v; }
        public double getTotalLitres() { return totalLitres; }
        public void setTotalLitres(double v) { this.totalLitres = v; }
        public double getTotalCout() { return totalCout; }
        public void setTotalCout(double v) { this.totalCout = v; }
        public double getTotalBudget() { return totalBudget; }
        public void setTotalBudget(double v) { this.totalBudget = v; }
        public double getTauxBudget() { return tauxBudget; }
        public void setTauxBudget(double v) { this.tauxBudget = v; }
        public int getNbAnomalies() { return nbAnomalies; }
        public void setNbAnomalies(int v) { this.nbAnomalies = v; }
        public double getRendementMoyen() { return rendementMoyen; }
        public void setRendementMoyen(double v) { this.rendementMoyen = v; }
    }

    // Évolution mensuelle globale (12 mois)
    public static class MoisStat {
        private int mois;
        private String label;
        private double totalKm;
        private double totalLitres;
        private double totalCout;
        private double totalBudget;
        private int nbVehicules;

        public MoisStat() {}
        public int getMois() { return mois; }
        public void setMois(int v) { this.mois = v; }
        public String getLabel() { return label; }
        public void setLabel(String v) { this.label = v; }
        public double getTotalKm() { return totalKm; }
        public void setTotalKm(double v) { this.totalKm = v; }
        public double getTotalLitres() { return totalLitres; }
        public void setTotalLitres(double v) { this.totalLitres = v; }
        public double getTotalCout() { return totalCout; }
        public void setTotalCout(double v) { this.totalCout = v; }
        public double getTotalBudget() { return totalBudget; }
        public void setTotalBudget(double v) { this.totalBudget = v; }
        public int getNbVehicules() { return nbVehicules; }
        public void setNbVehicules(int v) { this.nbVehicules = v; }
    }

    private List<ZoneStat> statsParZone;
    private List<MoisStat> evolutionMensuelle;
    private Map<String, Double> consommationParTypeCarburant;  // "GASOIL_ORDINAIRE" -> 1250.5 L
    private List<AnomalieDto> dernieresAnomalies;              // top 10

    public DashboardCarburantDto() {}
    public int getNbVehiculesSaisis() { return nbVehiculesSaisis; }
    public void setNbVehiculesSaisis(int v) { this.nbVehiculesSaisis = v; }
    public double getTotalKm() { return totalKm; }
    public void setTotalKm(double v) { this.totalKm = v; }
    public double getTotalLitres() { return totalLitres; }
    public void setTotalLitres(double v) { this.totalLitres = v; }
    public double getTotalCoutDT() { return totalCoutDT; }
    public void setTotalCoutDT(double v) { this.totalCoutDT = v; }
    public double getTotalBudgetDT() { return totalBudgetDT; }
    public void setTotalBudgetDT(double v) { this.totalBudgetDT = v; }
    public double getTauxBudgetGlobal() { return tauxBudgetGlobal; }
    public void setTauxBudgetGlobal(double v) { this.tauxBudgetGlobal = v; }
    public int getNbBudgetsDepasses() { return nbBudgetsDepasses; }
    public void setNbBudgetsDepasses(int v) { this.nbBudgetsDepasses = v; }
    public int getNbAnomalies() { return nbAnomalies; }
    public void setNbAnomalies(int v) { this.nbAnomalies = v; }
    public List<ZoneStat> getStatsParZone() { return statsParZone; }
    public void setStatsParZone(List<ZoneStat> v) { this.statsParZone = v; }
    public List<MoisStat> getEvolutionMensuelle() { return evolutionMensuelle; }
    public void setEvolutionMensuelle(List<MoisStat> v) { this.evolutionMensuelle = v; }
    public Map<String, Double> getConsommationParTypeCarburant() { return consommationParTypeCarburant; }
    public void setConsommationParTypeCarburant(Map<String, Double> v) { this.consommationParTypeCarburant = v; }
    public List<AnomalieDto> getDernieresAnomalies() { return dernieresAnomalies; }
    public void setDernieresAnomalies(List<AnomalieDto> v) { this.dernieresAnomalies = v; }
}