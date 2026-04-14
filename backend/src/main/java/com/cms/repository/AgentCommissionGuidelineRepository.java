package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.AgentCommissionGuideline;
import com.cms.model.enums.LocalityType;

public interface AgentCommissionGuidelineRepository extends JpaRepository<AgentCommissionGuideline, Long> {

    List<AgentCommissionGuideline> findByAgentId(Long agentId);

    List<AgentCommissionGuideline> findByProgramId(Long programId);

    Optional<AgentCommissionGuideline> findByAgentIdAndProgramIdAndLocalityType(Long agentId, Long programId, LocalityType localityType);
}
