package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.SemesterResult;

@Repository
public interface SemesterResultRepository extends JpaRepository<SemesterResult, Long> {

    Optional<SemesterResult> findByStudentTermEnrollment_Id(Long enrollmentId);

    List<SemesterResult> findByStudentTermEnrollment_TermInstance_Id(Long termInstanceId);

    List<SemesterResult> findByStudentTermEnrollment_Student_Id(Long studentId);

    List<SemesterResult> findByStudentTermEnrollment_Cohort_Id(Long cohortId);
}
