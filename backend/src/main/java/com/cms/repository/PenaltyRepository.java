package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Penalty;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    List<Penalty> findBySemesterFeeId(Long semesterFeeId);

    List<Penalty> findByStudentId(Long studentId);

    List<Penalty> findByStudentIdAndIsPaidFalse(Long studentId);
}
