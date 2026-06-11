package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;

import jakarta.persistence.LockModeType;

public interface StudentFeeAllocationRepository extends JpaRepository<StudentFeeAllocation, Long> {

    Optional<StudentFeeAllocation> findByStudentId(Long studentId);

    /** Acquires a row-level write lock — use only inside a write @Transactional. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM StudentFeeAllocation a WHERE a.student.id = :studentId")
    Optional<StudentFeeAllocation> findByStudentIdForUpdate(@Param("studentId") Long studentId);

    List<StudentFeeAllocation> findByProgramId(Long programId);

    List<StudentFeeAllocation> findByStatus(FeeAllocationStatus status);

    boolean existsByStudentId(Long studentId);
}
