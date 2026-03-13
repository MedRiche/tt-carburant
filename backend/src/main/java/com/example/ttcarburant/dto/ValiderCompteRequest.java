package com.example.ttcarburant.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ValiderCompteRequest {

    @NotNull(message = "L'ID de l'utilisateur est obligatoire")
    private Long utilisateurId;

    @NotEmpty(message = "Au moins une zone doit être sélectionnée")
    private List<Long> zoneIds;

    // Constructeurs
    public ValiderCompteRequest() {}

    public ValiderCompteRequest(Long utilisateurId, List<Long> zoneIds) {
        this.utilisateurId = utilisateurId;
        this.zoneIds = zoneIds;
    }

    // Getters et Setters
    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public List<Long> getZoneIds() {
        return zoneIds;
    }

    public void setZoneIds(List<Long> zoneIds) {
        this.zoneIds = zoneIds;
    }
}