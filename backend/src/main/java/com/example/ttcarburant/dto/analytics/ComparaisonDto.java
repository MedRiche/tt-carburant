package com.example.ttcarburant.dto.analytics;

import java.util.List;

// ── ComparaisonDto ────────────────────────────────────────────────────────────
public class ComparaisonDto {

    public static class VehiculeRank {
        private String matricule;
        private String marqueModele;
        private String zoneNom;
        private double totalKm;
        private double totalLitres;
        private double totalCout;
        private double rendementLPour100km;   // litres / 100 km
        private double rendementDTPour100km;  // DT / 100 km
        private double tauxBudgetMoyen;       // % budget utilisé en moyenne
        private int nbMoisSaisis;
        private int nbAnomalies;

        public VehiculeRank() {}
        public String getMatricule() { return matricule; }
        public void setMatricule(String v) { this.matricule = v; }
        public String getMarqueModele() { return marqueModele; }
        public void setMarqueModele(String v) { this.marqueModele = v; }
        public String getZoneNom() { return zoneNom; }
        public void setZoneNom(String v) { this.zoneNom = v; }
        public double getTotalKm() { return totalKm; }
        public void setTotalKm(double v) { this.totalKm = v; }
        public double getTotalLitres() { return totalLitres; }
        public void setTotalLitres(double v) { this.totalLitres = v; }
        public double getTotalCout() { return totalCout; }
        public void setTotalCout(double v) { this.totalCout = v; }
        public double getRendementLPour100km() { return rendementLPour100km; }
        public void setRendementLPour100km(double v) { this.rendementLPour100km = v; }
        public double getRendementDTPour100km() { return rendementDTPour100km; }
        public void setRendementDTPour100km(double v) { this.rendementDTPour100km = v; }
        public double getTauxBudgetMoyen() { return tauxBudgetMoyen; }
        public void setTauxBudgetMoyen(double v) { this.tauxBudgetMoyen = v; }
        public int getNbMoisSaisis() { return nbMoisSaisis; }
        public void setNbMoisSaisis(int v) { this.nbMoisSaisis = v; }
        public int getNbAnomalies() { return nbAnomalies; }
        public void setNbAnomalies(int v) { this.nbAnomalies = v; }
    }

    private List<VehiculeRank> top5Consommation;     // plus grosse conso DT
    private List<VehiculeRank> top5KmParcourus;      // plus de km
    private List<VehiculeRank> meilleursRendements;  // meilleur L/100km
    private List<VehiculeRank> piresRendements;      // pire L/100km
    private List<VehiculeRank> plusGrandsBudgetDepasses;
    private double moyenneGlobaleRendement;
    private double seuilRendementAnomalie;           // seuil détection anomalie (L/100km)

    public ComparaisonDto() {}
    public List<VehiculeRank> getTop5Consommation() { return top5Consommation; }
    public void setTop5Consommation(List<VehiculeRank> v) { this.top5Consommation = v; }
    public List<VehiculeRank> getTop5KmParcourus() { return top5KmParcourus; }
    public void setTop5KmParcourus(List<VehiculeRank> v) { this.top5KmParcourus = v; }
    public List<VehiculeRank> getMeilleursRendements() { return meilleursRendements; }
    public void setMeilleursRendements(List<VehiculeRank> v) { this.meilleursRendements = v; }
    public List<VehiculeRank> getPiresRendements() { return piresRendements; }
    public void setPiresRendements(List<VehiculeRank> v) { this.piresRendements = v; }
    public List<VehiculeRank> getPlusGrandsBudgetDepasses() { return plusGrandsBudgetDepasses; }
    public void setPlusGrandsBudgetDepasses(List<VehiculeRank> v) { this.plusGrandsBudgetDepasses = v; }
    public double getMoyenneGlobaleRendement() { return moyenneGlobaleRendement; }
    public void setMoyenneGlobaleRendement(double v) { this.moyenneGlobaleRendement = v; }
    public double getSeuilRendementAnomalie() { return seuilRendementAnomalie; }
    public void setSeuilRendementAnomalie(double v) { this.seuilRendementAnomalie = v; }
}