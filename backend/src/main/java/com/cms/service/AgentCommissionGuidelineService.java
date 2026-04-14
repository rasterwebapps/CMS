package com.cms.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AgentCommissionGuidelineRequest;
import com.cms.dto.AgentCommissionGuidelineResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Agent;
import com.cms.model.AgentCommissionGuideline;
import com.cms.model.Program;
import com.cms.model.enums.LocalityType;
import com.cms.repository.AgentCommissionGuidelineRepository;
import com.cms.repository.AgentRepository;
import com.cms.repository.ProgramRepository;

@Service
@Transactional(readOnly = true)
public class AgentCommissionGuidelineService {

    private final AgentCommissionGuidelineRepository guidelineRepository;
    private final AgentRepository agentRepository;
    private final ProgramRepository programRepository;

    public AgentCommissionGuidelineService(AgentCommissionGuidelineRepository guidelineRepository,
                                            AgentRepository agentRepository,
                                            ProgramRepository programRepository) {
        this.guidelineRepository = guidelineRepository;
        this.agentRepository = agentRepository;
        this.programRepository = programRepository;
    }

    @Transactional
    public AgentCommissionGuidelineResponse create(AgentCommissionGuidelineRequest request) {
        Agent agent = agentRepository.findById(request.agentId())
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.agentId()));

        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        AgentCommissionGuideline guideline = new AgentCommissionGuideline(
            agent, program, request.localityType(), request.suggestedCommission()
        );

        AgentCommissionGuideline saved = guidelineRepository.save(guideline);
        return toResponse(saved);
    }

    public List<AgentCommissionGuidelineResponse> findAll() {
        return guidelineRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public AgentCommissionGuidelineResponse findById(Long id) {
        AgentCommissionGuideline guideline = guidelineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agent commission guideline not found with id: " + id));
        return toResponse(guideline);
    }

    public List<AgentCommissionGuidelineResponse> findByAgentId(Long agentId) {
        return guidelineRepository.findByAgentId(agentId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AgentCommissionGuidelineResponse> findByProgramId(Long programId) {
        return guidelineRepository.findByProgramId(programId).stream()
            .map(this::toResponse)
            .toList();
    }

    public Optional<AgentCommissionGuidelineResponse> findGuideline(Long agentId, Long programId, LocalityType localityType) {
        return guidelineRepository.findByAgentIdAndProgramIdAndLocalityType(agentId, programId, localityType)
            .map(this::toResponse);
    }

    @Transactional
    public AgentCommissionGuidelineResponse update(Long id, AgentCommissionGuidelineRequest request) {
        AgentCommissionGuideline guideline = guidelineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agent commission guideline not found with id: " + id));

        Agent agent = agentRepository.findById(request.agentId())
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.agentId()));

        Program program = programRepository.findById(request.programId())
            .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + request.programId()));

        guideline.setAgent(agent);
        guideline.setProgram(program);
        guideline.setLocalityType(request.localityType());
        guideline.setSuggestedCommission(request.suggestedCommission());

        AgentCommissionGuideline updated = guidelineRepository.save(guideline);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!guidelineRepository.existsById(id)) {
            throw new ResourceNotFoundException("Agent commission guideline not found with id: " + id);
        }
        guidelineRepository.deleteById(id);
    }

    private AgentCommissionGuidelineResponse toResponse(AgentCommissionGuideline g) {
        return new AgentCommissionGuidelineResponse(
            g.getId(),
            g.getAgent().getId(),
            g.getAgent().getName(),
            g.getProgram().getId(),
            g.getProgram().getName(),
            g.getLocalityType(),
            g.getSuggestedCommission(),
            g.getCreatedAt(),
            g.getUpdatedAt()
        );
    }
}
