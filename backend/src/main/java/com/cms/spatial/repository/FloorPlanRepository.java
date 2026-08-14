package com.cms.spatial.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.spatial.model.FloorPlan;

@Repository
public interface FloorPlanRepository extends JpaRepository<FloorPlan, Long> {

    List<FloorPlan> findByEntityTypeAndEntityIdAndIsActiveTrue(String entityType, Long entityId);
}
