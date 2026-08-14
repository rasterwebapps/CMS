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
    List<StudentTermEnrollment> findByTermInstanceIdAndSemesterNumber(Long termInstanceId, Integer termNumber);

    /** Scoped by the elective group's own course (every CurriculumVersion is mandatorily
     *  course-scoped) so it never mixes in another program/course's students who happen to share
     *  the same termInstance+semesterNumber -- see CourseOfferingServiceImpl's
     *  resolveActiveCurriculumVersion for the same course-scoping rationale. */
    List<StudentTermEnrollment> findByTermInstanceIdAndSemesterNumberAndCohortCourseIdAndStatus(
        Long termInstanceId, Integer semesterNumber, Long courseId, EnrollmentStatus status);

    List<StudentTermEnrollment> findByCohortIdAndStatus(Long cohortId, EnrollmentStatus status);

    long countByTermInstanceIdAndCohortIdAndStatus(Long termInstanceId, Long cohortId, EnrollmentStatus status);

    Optional<StudentTermEnrollment> findFirstByTermInstanceIdAndCohortIdAndStatus(
        Long termInstanceId, Long cohortId, EnrollmentStatus status);
}
