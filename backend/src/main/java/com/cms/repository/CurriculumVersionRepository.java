package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CurriculumVersion;

public interface CurriculumVersionRepository extends JpaRepository<CurriculumVersion, Long> {

    List<CurriculumVersion> findByProgramId(Long programId);

    List<CurriculumVersion> findByProgramIdAndIsActiveTrue(Long programId);

    boolean existsByProgramIdAndIsActiveTrue(Long programId);

    /** Course-specific active versions — takes precedence over the program-wide fallback below. */
    List<CurriculumVersion> findByProgramIdAndCourseIdAndIsActiveTrue(Long programId, Long courseId);

    /** Program-wide active versions (course_id IS NULL) — used when no course-specific version exists. */
    List<CurriculumVersion> findByProgramIdAndCourseIdIsNullAndIsActiveTrue(Long programId);
}
