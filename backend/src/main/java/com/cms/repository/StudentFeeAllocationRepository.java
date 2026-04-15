package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;

public interface StudentFeeAllocationRepository extends JpaRepository<StudentFeeAllocation, Long> {

    Optional<StudentFeeAllocation> findByStudentId(Long studentId);

    List<StudentFeeAllocation> findByProgramId(Long programId);

    List<StudentFeeAllocation> findByStatus(FeeAllocationStatus status);

    boolean existsByStudentId(Long studentId);
}
