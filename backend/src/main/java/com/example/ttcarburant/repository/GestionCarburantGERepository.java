package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.GestionCarburantGE;
import com.example.ttcarburant.model.entity.GroupeElectrogene;
import com.example.ttcarburant.model.enums.Semestre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GestionCarburantGERepository extends JpaRepository<GestionCarburantGE, Long> {

    Optional<GestionCarburantGE> findByGroupeElectrogeneAndAnneeAndSemestre(GroupeElectrogene ge, int annee, Semestre semestre);

    List<GestionCarburantGE> findByGroupeElectrogeneOrderByAnneeDescSemestreDesc(GroupeElectrogene ge);

    List<GestionCarburantGE> findByAnneeAndSemestreOrderByGroupeElectrogene_Site(int annee, Semestre semestre);

    // ⚠️ CORRECTED: Use property traversal through groupeElectrogene.zone.id
    List<GestionCarburantGE> findByGroupeElectrogene_Zone_IdAndAnneeAndSemestre(Long zoneId, int annee, Semestre semestre);

    // Alternative: using @Query for clarity
    @Query("SELECT g FROM GestionCarburantGE g WHERE g.groupeElectrogene.zone.id = :zoneId AND g.annee = :annee AND g.semestre = :semestre")
    List<GestionCarburantGE> findByZoneAndPeriode(@Param("zoneId") Long zoneId, @Param("annee") int annee, @Param("semestre") Semestre semestre);

    @Query("SELECT g FROM GestionCarburantGE g WHERE g.groupeElectrogene.zone.id = :zoneId AND g.annee = :annee")
    List<GestionCarburantGE> findByZoneIdAndAnnee(@Param("zoneId") Long zoneId, @Param("annee") int annee);
}