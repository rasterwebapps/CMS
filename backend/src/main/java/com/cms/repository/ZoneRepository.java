package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Zone;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    List<Zone> findByFloorIdOrderByOrderIndexAsc(Long floorId);
    List<Zone> findByFloorIdAndIsActiveTrueOrderByOrderIndexAsc(Long floorId);
    List<Zone> findByIsActiveTrueOrderByNameAsc();

    boolean existsByFloorIdAndNameIgnoreCase(Long floorId, String name);
    boolean existsByFloorIdAndNameIgnoreCaseAndIdNot(Long floorId, String name, Long id);
}
