package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.GestionCarburantVehicule;
import com.example.ttcarburant.model.entity.Vehicule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarburantVehiculeRepository extends JpaRepository<GestionCarburantVehicule, Long> {

    List<GestionCarburantVehicule> findByVehiculeOrderByAnneeDescMoisDesc(Vehicule vehicule);

    List<GestionCarburantVehicule> findByAnneeAndMoisOrderByVehicule_Matricule(int annee, int mois);

    Optional<GestionCarburantVehicule> findByVehiculeAndAnneeAndMois(Vehicule v, int annee, int mois);

    // Tous les enregistrements d'une zone pour un mois donné
    @Query("SELECT g FROM GestionCarburantVehicule g WHERE g.vehicule.zone.id = :zoneId " +
            "AND g.annee = :annee AND g.mois = :mois ORDER BY g.vehicule.matricule")
    List<GestionCarburantVehicule> findByZoneAndPeriode(
            @Param("zoneId") Long zoneId,
            @Param("annee")  int annee,
            @Param("mois")   int mois);

    // Historique d'un véhicule par année
    List<GestionCarburantVehicule> findByVehiculeAndAnneeOrderByMois(Vehicule v, int annee);
}