package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.AgentRequest;
import com.cms.dto.AgentResponse;
import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.dto.ActiveStatusUpdateResponse;
import com.cms.exception.LifecycleConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Agent;
import com.cms.repository.AgentCommissionGuidelineRepository;
import com.cms.repository.AgentRepository;
import com.cms.repository.CommissionPayoutRepository;
import com.cms.repository.EnquiryRepository;

@Service
@Transactional(readOnly = true)
public class AgentService {

    private final AgentRepository agentRepository;
    private final EnquiryRepository enquiryRepository;
    private final AgentCommissionGuidelineRepository agentCommissionGuidelineRepository;
    private final CommissionPayoutRepository commissionPayoutRepository;

    public AgentService(AgentRepository agentRepository,
                        EnquiryRepository enquiryRepository,
                        AgentCommissionGuidelineRepository agentCommissionGuidelineRepository,
                        CommissionPayoutRepository commissionPayoutRepository) {
        this.agentRepository = agentRepository;
        this.enquiryRepository = enquiryRepository;
        this.agentCommissionGuidelineRepository = agentCommissionGuidelineRepository;
        this.commissionPayoutRepository = commissionPayoutRepository;
    }

    @Transactional
    public AgentResponse create(AgentRequest request) {
        Boolean isActive = request.isActive() != null ? request.isActive() : true;
        String name = requireTrimmed(request.name(), "Agent name is required");

        if (agentRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                "An agent with the name '" + name + "' already exists");
        }

        Agent agent = new Agent(
            name, trim(request.phone()), trim(request.email()),
            trim(request.area()), trim(request.locality()), isActive
        );
        agent.setAllottedSeats(request.allottedSeats());
        agent.setCommissionAmount(request.commissionAmount());
        applyIdentityAndBank(agent, request);

        Agent saved = agentRepository.save(agent);
        return toResponse(saved);
    }

    public List<AgentResponse> findAll() {
        return agentRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AgentResponse> findAll(String search, Sort sort) {
        Specification<Agent> spec = Specification.where(null);
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("area")), pattern)
            ));
        }
        return agentRepository.findAll(spec, sort).stream().map(this::toResponse).toList();
    }

    public Page<AgentResponse> findPage(String search, Pageable pageable) {
        Specification<Agent> spec = Specification.where(null);
        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("area")), pattern)
            ));
        }
        return agentRepository.findAll(spec, pageable).map(this::toResponse);
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
        String name = requireTrimmed(request.name(), "Agent name is required");

        if (agentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                "An agent with the name '" + name + "' already exists");
        }

        agent.setName(name);
        agent.setPhone(trim(request.phone()));
        agent.setEmail(trim(request.email()));
        agent.setArea(trim(request.area()));
        agent.setLocality(trim(request.locality()));
        agent.setAllottedSeats(request.allottedSeats());
        agent.setCommissionAmount(request.commissionAmount());

        if (request.isActive() != null) {
            agent.setIsActive(request.isActive());
        }

        applyIdentityAndBank(agent, request);

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

    @Transactional
    public ActiveStatusUpdateResponse updateStatus(Long id, ActiveStatusUpdateRequest request) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));
        if (Boolean.FALSE.equals(request.isActive())) {
            ensureCanDeactivateAgent(id);
        }
        agent.setIsActive(request.isActive());
        Agent updated = agentRepository.save(agent);
        return new ActiveStatusUpdateResponse(updated.getId(), updated.getIsActive(), updated.getUpdatedAt());
    }

    @Transactional
    public AgentResponse deactivate(Long id) {
        updateStatus(id, new ActiveStatusUpdateRequest(false, null));
        return findById(id);
    }

    @Transactional
    public AgentResponse reactivate(Long id) {
        updateStatus(id, new ActiveStatusUpdateRequest(true, null));
        return findById(id);
    }

    public boolean nameExists(String name, Long excludeId) {
        String trimmed = name == null ? "" : name.trim();
        if (excludeId != null) {
            return agentRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        }
        return agentRepository.existsByNameIgnoreCase(trimmed);
    }

    private void applyIdentityAndBank(Agent agent, AgentRequest r) {
        agent.setPanNumber(trim(r.panNumber()));
        agent.setAadhaarNumber(trim(r.aadhaarNumber()));
        agent.setBankAccountNumber(trim(r.bankAccountNumber()));
        agent.setBankIfscCode(trim(r.bankIfscCode()));
        agent.setBankBranch(trim(r.bankBranch()));
        agent.setBankName(trim(r.bankName()));
        agent.setBankAccountHolder(trim(r.bankAccountHolder()));
        agent.setBankAccountType(r.bankAccountType());
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireTrimmed(String s, String message) {
        String t = trim(s);
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
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
            agent.getCommissionAmount(),
            agent.getIsActive(),
            agent.getPanNumber(),
            agent.getAadhaarNumber(),
            agent.getBankAccountNumber(),
            agent.getBankIfscCode(),
            agent.getBankBranch(),
            agent.getBankName(),
            agent.getBankAccountHolder(),
            agent.getBankAccountType(),
            agent.getCreatedAt(),
            agent.getUpdatedAt()
        );
    }

    private void ensureCanDeactivateAgent(Long id) {
        if (enquiryRepository.existsByAgentId(id)) {
            throw new LifecycleConflictException(
                "Cannot deactivate Agent: enquiries are associated with it.",
                "ACTIVE_REFERENCE_EXISTS",
                "Agent",
                id,
                null
            );
        }
        if (agentCommissionGuidelineRepository.existsByAgentId(id)) {
            throw new LifecycleConflictException(
                "Cannot deactivate Agent: commission guidelines are associated with it.",
                "ACTIVE_REFERENCE_EXISTS",
                "Agent",
                id,
                null
            );
        }
        if (commissionPayoutRepository.existsByAgentId(id)) {
            throw new LifecycleConflictException(
                "Cannot deactivate Agent: commission payouts are associated with it.",
                "ACTIVE_REFERENCE_EXISTS",
                "Agent",
                id,
                null
            );
        }
    }
}
