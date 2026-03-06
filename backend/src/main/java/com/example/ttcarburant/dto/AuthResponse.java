package com.example.ttcarburant.dto;

import com.example.ttcarburant.model.enums.Role;
import com.example.ttcarburant.model.enums.StatutCompte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data

@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Long userId;
    private String nom;
    private String email;
    private Role role;
    private StatutCompte statutCompte;
    private String message;

    public AuthResponse() {}

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

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

    public StatutCompte getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(StatutCompte statutCompte) {
        this.statutCompte = statutCompte;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}




