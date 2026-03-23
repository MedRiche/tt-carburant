package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.Vehicule;
import com.example.ttcarburant.model.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehiculeRepository extends JpaRepository<Vehicule, String> {

    List<Vehicule> findByZone(Zone zone);

    List<Vehicule> findByZone_Id(Long zoneId);

    boolean existsByMatricule(String matricule);

    List<Vehicule> findByMarqueModeleContainingIgnoreCase(String marqueModele);

    List<Vehicule> findByTypeVehiculeIgnoreCase(String typeVehicule);
}