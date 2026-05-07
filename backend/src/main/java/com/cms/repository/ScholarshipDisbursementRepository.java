package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.ScholarshipDisbursement;

public interface ScholarshipDisbursementRepository extends JpaRepository<ScholarshipDisbursement, Long> {
    List<ScholarshipDisbursement> findByStudentScholarshipIdOrderByDisbursementDateDesc(Long studentScholarshipId);
    List<ScholarshipDisbursement> findByStudentScholarshipStudentIdOrderByDisbursementDateDesc(Long studentId);
}

