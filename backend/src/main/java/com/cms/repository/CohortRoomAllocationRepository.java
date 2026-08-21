package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CohortRoomAllocation;
import com.cms.model.enums.CohortRoomAllocationStatus;

public interface CohortRoomAllocationRepository extends JpaRepository<CohortRoomAllocation, Long> {
    Optional<CohortRoomAllocation> findByCohortIdAndTermInstanceIdAndStatus(
        Long cohortId, Long termInstanceId, CohortRoomAllocationStatus status);

    boolean existsByCohortIdAndTermInstanceIdAndStatus(
        Long cohortId, Long termInstanceId, CohortRoomAllocationStatus status);
}
