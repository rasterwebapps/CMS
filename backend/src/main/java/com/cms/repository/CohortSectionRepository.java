package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CohortSection;

public interface CohortSectionRepository extends JpaRepository<CohortSection, Long> {

    List<CohortSection> findByCohortRoomAllocationId(Long cohortRoomAllocationId);

    List<CohortSection> findByCohortRoomAllocationIdAndIsActiveTrue(Long cohortRoomAllocationId);

    List<CohortSection> findByTermInstanceIdAndIsActiveTrue(Long termInstanceId);
}
