package com.cms.spatial.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.spatial.model.VirtualLocation;

@Repository
public interface VirtualLocationRepository extends JpaRepository<VirtualLocation, Long> {

    List<VirtualLocation> findByFloorPlanIdAndIsActiveTrue(Long floorPlanId);

    List<VirtualLocation> findByEntityTypeAndEntityIdAndIsActiveTrue(String entityType, Long entityId);
}
