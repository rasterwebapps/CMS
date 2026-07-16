package com.cms.repository;

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
