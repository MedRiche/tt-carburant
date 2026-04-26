package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.enums.Role;
import com.example.ttcarburant.model.enums.StatutCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Utilisateur> findByStatutCompte(StatutCompte statutCompte);

    List<Utilisateur> findByStatutCompteOrderByDateCreationDesc(StatutCompte statutCompte);

    /** Conducteurs importés depuis Excel : role=TECHNICIEN + specialite=Conducteur */
    List<Utilisateur> findByStatutCompteAndSpecialiteOrderByDateCreationDesc(
            StatutCompte statutCompte, String specialite);

    List<Utilisateur> findByRoleOrderByDateCreationDesc(Role role);
}