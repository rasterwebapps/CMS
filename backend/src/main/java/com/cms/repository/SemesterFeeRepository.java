package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.SemesterFee;

public interface SemesterFeeRepository extends JpaRepository<SemesterFee, Long> {

    List<SemesterFee> findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(Long allocationId);

    @Query("SELECT sf FROM SemesterFee sf WHERE sf.allocation.student.id = :studentId " +
           "ORDER BY sf.yearNumber ASC, sf.semesterSequence ASC")
    List<SemesterFee> findByStudentIdOrderByYearNumberAndSemesterSequence(@Param("studentId") Long studentId);
}
