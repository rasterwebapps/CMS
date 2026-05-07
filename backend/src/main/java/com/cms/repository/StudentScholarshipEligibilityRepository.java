package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StudentScholarshipEligibility;

public interface StudentScholarshipEligibilityRepository extends JpaRepository<StudentScholarshipEligibility, Long> {
    Optional<StudentScholarshipEligibility> findByStudentId(Long studentId);
    boolean existsByStudentId(Long studentId);
}

