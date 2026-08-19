package com.cms.spatial.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.spatial.model.FloorPlan;

@Repository
public interface FloorPlanRepository extends JpaRepository<FloorPlan, Long> {

    List<FloorPlan> findByEntityTypeAndEntityIdAndIsActiveTrue(String entityType, Long entityId);

    /** Bulk existence check for a Campus Setup card grid — which of these entity ids already have
     *  at least one active floor plan, without an N-request round trip per card. */
    @Query("select distinct fp.entityId from FloorPlan fp "
        + "where fp.entityType = :entityType and fp.entityId in :entityIds and fp.isActive = true")
    List<Long> findEntityIdsWithFloorPlan(@Param("entityType") String entityType, @Param("entityIds") List<Long> entityIds);
}
