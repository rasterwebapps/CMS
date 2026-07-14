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

    /**
     * Subjects mapped into any curriculum term row that applies to the given course: either the
     * row itself restricts to this course, or (absent a row-level restriction) the curriculum
     * version restricts to this course, or (absent both) the row's curriculum version is
     * program-wide and this course belongs to that program.
     */
    @Query("select distinct csc.subject.id from CurriculumSemesterCourse csc "
        + "where csc.course.id = :courseId "
        + "or (csc.course is null and csc.curriculumVersion.course.id = :courseId) "
        + "or (csc.course is null and csc.curriculumVersion.course is null "
        + "    and csc.curriculumVersion.program.id = :programId)")
    List<Long> findDistinctSubjectIdsByCourseId(@Param("courseId") Long courseId, @Param("programId") Long programId);
}
