package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.cms.model.RollNumberSequence;

import jakarta.persistence.LockModeType;

public interface RollNumberSequenceRepository extends JpaRepository<RollNumberSequence, Long> {

    /**
     * Find roll number sequence with pessimistic write lock to prevent concurrent updates.
     * This ensures thread-safe sequence number generation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RollNumberSequence r WHERE r.courseId = :courseId AND r.academicYear = :academicYear")
    Optional<RollNumberSequence> findByCourseIdAndAcademicYearForUpdate(Long courseId, Integer academicYear);

    Optional<RollNumberSequence> findByCourseIdAndAcademicYear(Long courseId, Integer academicYear);
}

