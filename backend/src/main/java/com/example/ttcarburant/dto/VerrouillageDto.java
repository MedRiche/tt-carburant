// ══════════════════════════════════════════════════════════════════
// FILE: VerrouillageDto.java
// ══════════════════════════════════════════════════════════════════
package com.example.ttcarburant.dto;

import java.time.LocalDateTime;

public class VerrouillageDto {

    private Long id;
    private int annee;
    private int mois;
    private String moisLabel;
    private Long zoneId;
    private String zoneNom;
    private boolean verrouille;
    private String verrouilleParEmail;
    private LocalDateTime verrouillerLe;
    private String deverrouilleParEmail;
    private LocalDateTime deverrouillerLe;

    public VerrouillageDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }
    public int getMois() { return mois; }
    public void setMois(int mois) { this.mois = mois; }
    public String getMoisLabel() { return moisLabel; }
    public void setMoisLabel(String moisLabel) { this.moisLabel = moisLabel; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getZoneNom() { return zoneNom; }
    public void setZoneNom(String zoneNom) { this.zoneNom = zoneNom; }
    public boolean isVerrouille() { return verrouille; }
    public void setVerrouille(boolean verrouille) { this.verrouille = verrouille; }
    public String getVerrouilleParEmail() { return verrouilleParEmail; }
    public void setVerrouilleParEmail(String v) { this.verrouilleParEmail = v; }
    public LocalDateTime getVerrouillerLe() { return verrouillerLe; }
    public void setVerrouillerLe(LocalDateTime v) { this.verrouillerLe = v; }
    public String getDeverrouilleParEmail() { return deverrouilleParEmail; }
    public void setDeverrouilleParEmail(String v) { this.deverrouilleParEmail = v; }
    public LocalDateTime getDeverrouillerLe() { return deverrouillerLe; }
    public void setDeverrouillerLe(LocalDateTime v) { this.deverrouillerLe = v; }
}

// ══════════════════════════════════════════════════════════════════
// FILE: HistoriqueModificationDto.java
// ══════════════════════════════════════════════════════════════════
// (Separate class — put in its own file in production)

// package com.example.ttcarburant.dto;
// import java.time.LocalDateTime;
// public class HistoriqueModificationDto { ... }