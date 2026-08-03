package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.SyllabusUnitPlan;

public interface SyllabusUnitPlanRepository extends JpaRepository<SyllabusUnitPlan, Long> {

    List<SyllabusUnitPlan> findByCourseOfferingIdOrderBySequenceIndexAsc(Long courseOfferingId);

    void deleteByCourseOfferingId(Long courseOfferingId);
}
