package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StudentScholarship;
import com.cms.model.enums.ScholarshipStatus;

public interface StudentScholarshipRepository extends JpaRepository<StudentScholarship, Long> {
    List<StudentScholarship> findByStudentIdOrderByAcademicYearStartDateDesc(Long studentId);
    List<StudentScholarship> findByStudentIdAndStatus(Long studentId, ScholarshipStatus status);
    Optional<StudentScholarship> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
    List<StudentScholarship> findByStatusAndScholarshipTypeId(ScholarshipStatus status, Long scholarshipTypeId);
    List<StudentScholarship> findByStatusOrderByApplicationDateAsc(ScholarshipStatus status);
    boolean existsByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
}

