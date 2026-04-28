package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.IntakeRule;

public interface IntakeRuleRepository extends JpaRepository<IntakeRule, Long> {
    List<IntakeRule> findByProgramId(Long programId);
    List<IntakeRule> findByProgramIdAndIsActiveTrue(Long programId);
}
