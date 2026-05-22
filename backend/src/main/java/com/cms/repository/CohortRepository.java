package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Cohort;
import com.cms.model.enums.CohortStatus;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    Optional<Cohort> findByCourseIdAndAdmissionAcademicYearId(Long courseId, Long academicYearId);
    List<Cohort> findByStatus(CohortStatus status);
    Optional<Cohort> findByCohortCode(String cohortCode);
    List<Cohort> findByAdmissionAcademicYearId(Long academicYearId);
}
