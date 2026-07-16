package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.CurriculumSemesterCourse;

public interface CurriculumSemesterCourseRepository extends JpaRepository<CurriculumSemesterCourse, Long> {

    List<CurriculumSemesterCourse> findByCurriculumVersionId(Long curriculumVersionId);

    List<CurriculumSemesterCourse> findByCurriculumVersionIdAndSemesterNumber(
        Long curriculumVersionId, Integer termNumber);

    boolean existsByCurriculumVersionId(Long curriculumVersionId);

    boolean existsByElectiveGroupId(Long electiveGroupId);

    boolean existsBySubjectId(Long subjectId);

    @Query("select count(distinct csc.semesterNumber) from CurriculumSemesterCourse csc "
        + "where csc.curriculumVersion.id = :curriculumVersionId")
    long countDistinctTermsByCurriculumVersionId(@Param("curriculumVersionId") Long curriculumVersionId);

    @Query("select count(distinct csc.subject.id) from CurriculumSemesterCourse csc "
        + "where csc.curriculumVersion.id = :curriculumVersionId")
    long countDistinctSubjectsByCurriculumVersionId(@Param("curriculumVersionId") Long curriculumVersionId);

    /** Subjects mapped into any curriculum term row whose curriculum version is scoped to this course. */
    @Query("select distinct csc.subject.id from CurriculumSemesterCourse csc "
        + "where csc.curriculumVersion.course.id = :courseId")
    List<Long> findDistinctSubjectIdsByCourseId(@Param("courseId") Long courseId);
}
