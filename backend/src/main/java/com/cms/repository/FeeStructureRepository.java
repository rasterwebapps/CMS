package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.FeeStructure;
import com.cms.model.enums.FeeType;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    List<FeeStructure> findByProgramId(Long programId);

    List<FeeStructure> findByAcademicYearId(Long academicYearId);

    List<FeeStructure> findByProgramIdAndAcademicYearId(Long programId, Long academicYearId);

    List<FeeStructure> findByProgramIdAndAcademicYearIdAndIsActiveTrue(Long programId, Long academicYearId);

    List<FeeStructure> findByFeeType(FeeType feeType);

    List<FeeStructure> findByIsActiveTrue();

    List<FeeStructure> findByProgramIdAndCourseId(Long programId, Long courseId);

    List<FeeStructure> findByProgramIdAndCourseIdAndAcademicYearId(Long programId, Long courseId, Long academicYearId);

    List<FeeStructure> findByProgramIdAndAcademicYearIdAndCourseIsNull(Long programId, Long academicYearId);

    List<FeeStructure> findByCourseIsNull();
}
