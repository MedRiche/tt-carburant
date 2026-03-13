package com.example.ttcarburant.dto;

import com.example.ttcarburant.model.enums.Role;
import com.example.ttcarburant.model.enums.StatutCompte;

import java.time.LocalDateTime;
import java.util.List;

public class UtilisateurDto {

    private Long id;
    private String nom;
    private String email;
    private Role role;
    private StatutCompte statutCompte;
    private LocalDateTime dateCreation;
    private String specialite;
    private List<ZoneDto> zones;

    public UtilisateurDto() {}

    public UtilisateurDto(Long id, String nom, String email, Role role,
                          StatutCompte statutCompte, LocalDateTime dateCreation,
                          String specialite, List<ZoneDto> zones) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.role = role;
        this.statutCompte = statutCompte;
        this.dateCreation = dateCreation;
        this.specialite = specialite;
        this.zones = zones;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public StatutCompte getStatutCompte() { return statutCompte; }
    public void setStatutCompte(StatutCompte statutCompte) { this.statutCompte = statutCompte; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public List<ZoneDto> getZones() { return zones; }
    public void setZones(List<ZoneDto> zones) { this.zones = zones; }
}