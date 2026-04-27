package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StudentTermEnrollment;
import com.cms.model.enums.EnrollmentStatus;

public interface StudentTermEnrollmentRepository extends JpaRepository<StudentTermEnrollment, Long> {
    List<StudentTermEnrollment> findByTermInstanceId(Long termInstanceId);
    List<StudentTermEnrollment> findByTermInstanceIdAndStatus(Long termInstanceId, EnrollmentStatus status);
    List<StudentTermEnrollment> findByStudentId(Long studentId);
    List<StudentTermEnrollment> findByTermInstanceIdAndCohortId(Long termInstanceId, Long cohortId);
    Optional<StudentTermEnrollment> findByStudentIdAndTermInstanceId(Long studentId, Long termInstanceId);
    List<StudentTermEnrollment> findByTermInstanceIdAndSemesterNumber(Long termInstanceId, Integer semesterNumber);
}
