package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.ClinicalShiftGroup;

public interface ClinicalShiftGroupRepository extends JpaRepository<ClinicalShiftGroup, Long> {

    List<ClinicalShiftGroup> findByCourseOfferingId(Long courseOfferingId);

    List<ClinicalShiftGroup> findByTermInstanceIdAndIsActiveTrue(Long termInstanceId);
}
