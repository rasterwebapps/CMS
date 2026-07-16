package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.CurriculumVersion;

public interface CurriculumVersionRepository extends JpaRepository<CurriculumVersion, Long>,
    JpaSpecificationExecutor<CurriculumVersion> {

    List<CurriculumVersion> findByProgramId(Long programId);

    List<CurriculumVersion> findByProgramIdAndIsActiveTrue(Long programId);

    boolean existsByProgramIdAndIsActiveTrue(Long programId);

    /** Active curriculum versions scoped to this exact course. */
    List<CurriculumVersion> findByProgramIdAndCourseIdAndIsActiveTrue(Long programId, Long courseId);

    /** Uniqueness scoped to program+course; excludeId used when editing. */
    @Query("select case when count(cv) > 0 then true else false end from CurriculumVersion cv "
        + "where cv.program.id = :programId "
        + "and cv.course.id = :courseId "
        + "and lower(cv.versionName) = lower(:versionName) "
        + "and (:excludeId is null or cv.id <> :excludeId)")
    boolean existsByProgramAndCourseAndVersionName(@Param("programId") Long programId,
                                                    @Param("courseId") Long courseId,
                                                    @Param("versionName") String versionName,
                                                    @Param("excludeId") Long excludeId);
}
