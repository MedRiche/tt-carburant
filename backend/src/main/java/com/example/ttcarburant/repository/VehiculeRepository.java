package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehiculeRepository extends JpaRepository<Vehicule, String> {

    List<Vehicule> findByZone(Zone zone);

    List<Vehicule> findByZone_Id(Long zoneId);

    boolean existsByMatricule(String matricule);

    List<Vehicule> findByMarqueModeleContainingIgnoreCase(String marqueModele);

    List<Vehicule> findByTypeVehiculeIgnoreCase(String typeVehicule);

    /**
     * Recherche les véhicules d'un conducteur par son nom et prénom (insensible à la casse).
     * Utilisé pour les conducteurs qui voient uniquement leurs véhicules assignés.
     */
    @Query("SELECT v FROM Vehicule v WHERE " +
            "LOWER(FUNCTION('REPLACE', FUNCTION('REPLACE', v.nomConducteur, 'é','e'), 'è','e')) " +
            "LIKE LOWER(CONCAT('%', :nom, '%')) OR " +
            "LOWER(FUNCTION('REPLACE', FUNCTION('REPLACE', v.prenomConducteur, 'é','e'), 'è','e')) " +
            "LIKE LOWER(CONCAT('%', :prenom, '%'))")
    List<Vehicule> findByConducteurNomOuPrenom(
            @Param("nom") String nom,
            @Param("prenom") String prenom);

    /**
     * Recherche exacte par nom + prénom du conducteur (insensible casse).
     */
    @Query("SELECT v FROM Vehicule v WHERE " +
            "LOWER(v.nomConducteur) = LOWER(:nom) AND " +
            "LOWER(v.prenomConducteur) = LOWER(:prenom)")
    List<Vehicule> findByConducteurNomEtPrenom(
            @Param("nom") String nom,
            @Param("prenom") String prenom);

    /**
     * Recherche de véhicules dont le conducteur correspond à un nom complet.
     * Ex: "TAOUFIK JEBRI" → match nomConducteur + prenomConducteur
     */
    @Query("SELECT v FROM Vehicule v WHERE " +
            "LOWER(CONCAT(v.prenomConducteur, ' ', v.nomConducteur)) LIKE LOWER(CONCAT('%', :nomComplet, '%')) OR " +
            "LOWER(CONCAT(v.nomConducteur, ' ', v.prenomConducteur)) LIKE LOWER(CONCAT('%', :nomComplet, '%'))")
    List<Vehicule> findByConducteurNomComplet(@Param("nomComplet") String nomComplet);
}