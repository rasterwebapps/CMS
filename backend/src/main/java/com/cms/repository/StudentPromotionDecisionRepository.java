package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StudentPromotionDecision;

public interface StudentPromotionDecisionRepository extends JpaRepository<StudentPromotionDecision, Long> {
    List<StudentPromotionDecision> findByCohortId(Long cohortId);
    List<StudentPromotionDecision> findByStudentId(Long studentId);
}
