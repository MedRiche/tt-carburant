package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.HistoriqueModificationCarburant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueModificationCarburantRepository extends JpaRepository<HistoriqueModificationCarburant, Long> {

    /** Historique d'un enregistrement spécifique */
    List<HistoriqueModificationCarburant> findByGestionIdOrderByModifieLeDesc(Long gestionId);

    /** Historique d'un véhicule */
    List<HistoriqueModificationCarburant> findByVehiculeMatriculeOrderByModifieLeDesc(String matricule);

    /** Historique d'un mois/année */
    List<HistoriqueModificationCarburant> findByAnneeAndMoisOrderByModifieLeDesc(int annee, int mois);

    /** Historique récent global */
    @Query("SELECT h FROM HistoriqueModificationCarburant h ORDER BY h.modifieLe DESC")
    List<HistoriqueModificationCarburant> findAllOrderByDateDesc();

    /** Historique par utilisateur */
    List<HistoriqueModificationCarburant> findByModifieParOrderByModifieLeDesc(String email);

    /** Historique d'un véhicule + année */
    List<HistoriqueModificationCarburant> findByVehiculeMatriculeAndAnneeOrderByModifieLeDesc(
            String matricule, int annee);
}