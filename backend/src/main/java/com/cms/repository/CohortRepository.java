package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cms.model.Cohort;
import com.cms.model.enums.CohortStatus;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    Optional<Cohort> findByCourseIdAndAdmissionAcademicYearId(Long courseId, Long academicYearId);
    List<Cohort> findByStatus(CohortStatus status);
    Optional<Cohort> findByCohortCode(String cohortCode);
    List<Cohort> findByAdmissionAcademicYearId(Long academicYearId);
    List<Cohort> findByAdmissionAcademicYearIdAndCounsellingClosedTrue(Long academicYearId);

    @Query("SELECT c FROM Cohort c LEFT JOIN FETCH c.course WHERE c.admissionAcademicYear.id = :academicYearId")
    List<Cohort> findByAdmissionAcademicYearIdWithCourse(Long academicYearId);
}
