package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.AffectationUtilisateurZone;
import com.example.ttcarburant.model.entity.Utilisateur;
import com.example.ttcarburant.model.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AffectationUtilisateurZoneRepository extends JpaRepository<AffectationUtilisateurZone, Long> {

    List<AffectationUtilisateurZone> findByUtilisateur(Utilisateur utilisateur);

    List<AffectationUtilisateurZone> findByZone(Zone zone);

    boolean existsByUtilisateurAndZone(Utilisateur utilisateur, Zone zone);

    void deleteByUtilisateur(Utilisateur utilisateur);

    void deleteByUtilisateurAndZone(Utilisateur utilisateur, Zone zone);
}