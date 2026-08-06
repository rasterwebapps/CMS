package com.cms.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.CourseOffering;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    List<CourseOffering> findByTermInstanceId(Long termInstanceId);

    List<CourseOffering> findByTermInstanceIdAndSemesterNumber(Long termInstanceId, Integer termNumber);

    List<CourseOffering> findByTermInstanceIdAndCurriculumVersionId(Long termInstanceId, Long cvId);

    /** Cohort-scoped read — the actual fix for cross-program/curriculum leakage. Unlike
     *  {@link #findByTermInstanceIdAndSemesterNumber}, this also pins {@code curriculumVersionId}
     *  so a different cohort/program's offerings sharing the same TermInstance+semesterNumber
     *  never show up together, even though the underlying row can legitimately be shared by
     *  several cohorts on the same curriculum version (see the idempotent-create check in
     *  CourseOfferingServiceImpl#generateOfferingsForTermInstance). */
    List<CourseOffering> findByTermInstanceIdAndCurriculumVersionIdAndSemesterNumberIn(
        Long termInstanceId, Long curriculumVersionId, Collection<Integer> semesterNumbers);

    List<CourseOffering> findByTermInstanceIdAndIsActiveTrue(Long termInstanceId);

    Optional<CourseOffering> findByTermInstanceIdAndCurriculumVersionIdAndSubjectIdAndSemesterNumber(
        Long termInstanceId, Long curriculumVersionId, Long subjectId, Integer termNumber);

    List<CourseOffering> findByTermInstanceIdAndSubjectId(Long termInstanceId, Long subjectId);

    List<CourseOffering> findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(
        Long termInstanceId, Long electiveGroupId);

    boolean existsBySubjectId(Long subjectId);

    boolean existsByCurriculumVersionId(Long curriculumVersionId);

    boolean existsByCurriculumSemesterCourseId(Long curriculumSemesterCourseId);

    @Query("select distinct co.curriculumSemesterCourse.id from CourseOffering co "
        + "where co.curriculumVersion.id = :curriculumVersionId and co.curriculumSemesterCourse is not null")
    Set<Long> findLockedCurriculumSemesterCourseIds(@Param("curriculumVersionId") Long curriculumVersionId);
}
