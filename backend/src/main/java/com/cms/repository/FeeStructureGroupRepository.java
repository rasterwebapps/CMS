package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.FeeStructureGroup;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.Gender;

public interface FeeStructureGroupRepository extends JpaRepository<FeeStructureGroup, Long> {

    @Query("""
        SELECT g FROM FeeStructureGroup g
        WHERE g.program.id = :programId
          AND g.academicYear.id = :academicYearId
          AND (:courseId IS NULL AND g.course IS NULL OR g.course.id = :courseId)
          AND g.quota = :quota
          AND g.feeState.id = :feeStateId
          AND g.gender = :gender
        """)
    Optional<FeeStructureGroup> findExact(
        @Param("programId") Long programId,
        @Param("academicYearId") Long academicYearId,
        @Param("courseId") Long courseId,
        @Param("quota") AdmissionQuota quota,
        @Param("feeStateId") Long feeStateId,
        @Param("gender") Gender gender
    );

    List<FeeStructureGroup> findByProgramIdAndAcademicYearId(Long programId, Long academicYearId);

    List<FeeStructureGroup> findByProgramId(Long programId);

    List<FeeStructureGroup> findByAcademicYearId(Long academicYearId);

    boolean existsByProgramId(Long programId);

    boolean existsByAcademicYearId(Long academicYearId);

    boolean existsByCourseId(Long courseId);

    boolean existsByFeeStateId(Long feeStateId);
}
