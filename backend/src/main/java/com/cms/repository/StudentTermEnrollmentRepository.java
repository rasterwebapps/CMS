package com.cms.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Every cohort with at least one ENROLLED student in this term -- the "which cohorts are
     *  actually active here" source for the Global Auto-Scheduler's cross-cohort loop. */
    @Query("select distinct e.cohort.id from StudentTermEnrollment e "
        + "where e.termInstance.id = :termInstanceId and e.status = :status")
    Set<Long> findDistinctCohortIdsByTermInstanceId(@Param("termInstanceId") Long termInstanceId, @Param("status") EnrollmentStatus status);
}
