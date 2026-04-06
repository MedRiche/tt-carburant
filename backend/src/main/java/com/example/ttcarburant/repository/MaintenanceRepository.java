package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.Maintenance;
import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.enums.StatutMaintenance;
import com.example.ttcarburant.model.enums.TypeIntervention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    // ── Recherches basiques ────────────────────────────────────────────────

    List<Maintenance> findByVehiculeOrderByDateInterventionDesc(Vehicule vehicule);

    List<Maintenance> findByVehicule_MatriculeOrderByDateInterventionDesc(String matricule);

    List<Maintenance> findByStatutOrderByDateCreationDesc(StatutMaintenance statut);

    List<Maintenance> findByTypeInterventionOrderByDateCreationDesc(TypeIntervention type);

    Optional<Maintenance> findByNumeroDossierAndVehicule_Matricule(String numeroDossier, String matricule);

    // ── Par zone ──────────────────────────────────────────────────────────

    @Query("SELECT m FROM Maintenance m WHERE m.vehicule.zone.id = :zoneId ORDER BY m.dateCreation DESC")
    List<Maintenance> findByZoneId(@Param("zoneId") Long zoneId);

    @Query("SELECT m FROM Maintenance m WHERE m.vehicule.zone.id = :zoneId AND m.statut = :statut ORDER BY m.dateCreation DESC")
    List<Maintenance> findByZoneIdAndStatut(@Param("zoneId") Long zoneId, @Param("statut") StatutMaintenance statut);

    // ── Analytics ─────────────────────────────────────────────────────────

    @Query("SELECT m.vehicule.matricule, SUM(m.coutTotalHtva) FROM Maintenance m " +
            "GROUP BY m.vehicule.matricule ORDER BY SUM(m.coutTotalHtva) DESC")
    List<Object[]> getCoutParVehicule();

    @Query("SELECT m.vehicule.zone.nom, SUM(m.coutTotalHtva) FROM Maintenance m " +
            "WHERE m.vehicule.zone IS NOT NULL GROUP BY m.vehicule.zone.nom ORDER BY SUM(m.coutTotalHtva) DESC")
    List<Object[]> getCoutParZone();

    @Query("SELECT d.marque, SUM(d.totalHtva) FROM DetailMaintenance d " +
            "WHERE d.marque IS NOT NULL GROUP BY d.marque ORDER BY SUM(d.totalHtva) DESC")
    List<Object[]> getCoutParPrestataire();

    @Query("SELECT COUNT(m), SUM(m.coutTotalHtva) FROM Maintenance m")
    Object[] getGlobalStats();

    @Query("SELECT m.typeIntervention, COUNT(m), SUM(m.coutTotalHtva) FROM Maintenance m GROUP BY m.typeIntervention")
    List<Object[]> getStatsParType();

    // ── Recherche texte ───────────────────────────────────────────────────

    @Query("SELECT m FROM Maintenance m WHERE " +
            "LOWER(m.numeroDossier) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(m.vehicule.matricule) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(m.vehicule.marqueModele) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "ORDER BY m.dateCreation DESC")
    List<Maintenance> search(@Param("q") String query);
}