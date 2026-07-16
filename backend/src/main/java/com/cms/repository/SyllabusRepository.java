package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.Syllabus;

public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {

    List<Syllabus> findByCurriculumSemesterCourse_Subject_Id(Long subjectId);

    Optional<Syllabus> findByCurriculumSemesterCourse_Subject_IdAndIsActiveTrue(Long subjectId);

    List<Syllabus> findByCurriculumSemesterCourseId(Long curriculumSemesterCourseId);

    List<Syllabus> findByIsActiveTrue();

    @Query("SELECT COALESCE(MAX(s.version), 0) FROM Syllabus s WHERE s.curriculumSemesterCourse.id = :curriculumTermCourseId")
    Integer findMaxVersion(@Param("curriculumTermCourseId") Long curriculumTermCourseId);

    /** Mirrors AcademicYearRepository.clearCurrentAcademicYear()'s "only one active at a time" pattern,
     *  scoped to a single subject-in-this-term mapping instead of globally. */
    @Modifying
    @Query("UPDATE Syllabus s SET s.isActive = false "
        + "WHERE s.curriculumSemesterCourse.id = :curriculumTermCourseId AND s.isActive = true")
    void clearActiveForMapping(@Param("curriculumTermCourseId") Long curriculumTermCourseId);
}
