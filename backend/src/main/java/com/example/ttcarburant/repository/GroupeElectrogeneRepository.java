// com.example.ttcarburant.repository.GroupeElectrogeneRepository.java
package com.example.ttcarburant.repository;

import com.example.ttcarburant.model.entity.GroupeElectrogene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupeElectrogeneRepository extends JpaRepository<GroupeElectrogene, String> {
    Optional<GroupeElectrogene> findBySite(String site);
    boolean existsBySite(String site);
    List<GroupeElectrogene> findByZoneId(Long zoneId);
}