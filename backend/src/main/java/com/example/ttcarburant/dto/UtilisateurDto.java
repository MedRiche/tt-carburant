package com.example.ttcarburant.dto;

import com.example.ttcarburant.model.enums.Role;
import com.example.ttcarburant.model.enums.StatutCompte;

import java.time.LocalDateTime;

public class UtilisateurDto {

    private Long id;
    private String nom;
    private String email;
    private Role role;
    private StatutCompte statutCompte;
    private LocalDateTime dateCreation;
    private String specialite;

    public UtilisateurDto() {}

    public UtilisateurDto(Long id, String nom, String email, Role role,
                          StatutCompte statutCompte, LocalDateTime dateCreation,
                          String specialite) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.role = role;
        this.statutCompte = statutCompte;
        this.dateCreation = dateCreation;
        this.specialite = specialite;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public StatutCompte getStatutCompte() { return statutCompte; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public String getSpecialite() { return specialite; }

}