package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AgentRequest;
import com.cms.dto.AgentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Agent;
import com.cms.repository.AgentRepository;

@Service
@Transactional(readOnly = true)
public class AgentService {

    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Transactional
    public AgentResponse create(AgentRequest request) {
        Boolean isActive = request.isActive() != null ? request.isActive() : true;

        Agent agent = new Agent(
            request.name(), request.phone(), request.email(),
            request.area(), request.locality(), isActive
        );
        agent.setAllottedSeats(request.allottedSeats());

        Agent saved = agentRepository.save(agent);
        return toResponse(saved);
    }

    public List<AgentResponse> findAll() {
        return agentRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public AgentResponse findById(Long id) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));
        return toResponse(agent);
    }

    public List<AgentResponse> findActiveAgents() {
        return agentRepository.findByIsActiveTrue().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AgentResponse update(Long id, AgentRequest request) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));

        if (agentRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException(
                "An agent with the name '" + request.name() + "' already exists");
        }

        agent.setName(request.name());
        agent.setPhone(request.phone());
        agent.setEmail(request.email());
        agent.setArea(request.area());
        agent.setLocality(request.locality());
        agent.setAllottedSeats(request.allottedSeats());

        if (request.isActive() != null) {
            agent.setIsActive(request.isActive());
        }

        Agent updated = agentRepository.save(agent);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!agentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Agent not found with id: " + id);
        }
        agentRepository.deleteById(id);
    }

    private AgentResponse toResponse(Agent agent) {
        return new AgentResponse(
            agent.getId(),
            agent.getName(),
            agent.getPhone(),
            agent.getEmail(),
            agent.getArea(),
            agent.getLocality(),
            agent.getAllottedSeats(),
            agent.getIsActive(),
            agent.getCreatedAt(),
            agent.getUpdatedAt()
        );
    }
}
