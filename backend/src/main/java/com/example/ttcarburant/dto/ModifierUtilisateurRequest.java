package com.example.ttcarburant.dto;

import com.example.ttcarburant.model.enums.Role;

public class ModifierUtilisateurRequest {

    private String email;
    private Role role;
    private String specialite;

    public ModifierUtilisateurRequest() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }
}