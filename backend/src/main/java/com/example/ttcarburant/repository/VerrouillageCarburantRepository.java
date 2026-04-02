// ══════════════════════════════════════════════════════════════════
// FILE: VerrouillageCarburantRepository.java
// ══════════════════════════════════════════════════════════════════
package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.VerrouillageCarburant;
import com.example.ttcarburant.model.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerrouillageCarburantRepository extends JpaRepository<VerrouillageCarburant, Long> {

    /** Trouver le verrou pour un mois/zone exact */
    Optional<VerrouillageCarburant> findByAnneeAndMoisAndZone(int annee, int mois, Zone zone);

    /** Trouver le verrou global (sans zone) */
    Optional<VerrouillageCarburant> findByAnneeAndMoisAndZoneIsNull(int annee, int mois);

    /** Tous les verrous d'une année */
    List<VerrouillageCarburant> findByAnneeOrderByMoisAsc(int annee);

    /** Tous les verrous d'une zone */
    @Query("SELECT v FROM VerrouillageCarburant v WHERE v.zone.id = :zoneId ORDER BY v.annee DESC, v.mois DESC")
    List<VerrouillageCarburant> findByZoneIdOrderByDesc(@Param("zoneId") Long zoneId);

    /** Vérifier si un mois est verrouillé (global OU par zone) */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END " +
            "FROM VerrouillageCarburant v " +
            "WHERE v.annee = :annee AND v.mois = :mois AND v.verrouille = true " +
            "AND (v.zone IS NULL OR v.zone.id = :zoneId)")
    boolean isVerrouille(@Param("annee") int annee,
                         @Param("mois") int mois,
                         @Param("zoneId") Long zoneId);

    /** Tous les mois verrouillés */
    List<VerrouillageCarburant> findByVerrouilleTrue();
}