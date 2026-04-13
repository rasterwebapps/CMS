package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.LabContinuousEvaluation;

public interface LabContinuousEvaluationRepository extends JpaRepository<LabContinuousEvaluation, Long> {
    List<LabContinuousEvaluation> findByExperimentId(Long experimentId);
    List<LabContinuousEvaluation> findByStudentId(Long studentId);
}
