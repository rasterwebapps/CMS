package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ActiveStatusUpdateRequest;
import com.cms.exception.LifecycleConflictException;
import com.cms.model.Agent;
import com.cms.repository.AgentCommissionGuidelineRepository;
import com.cms.repository.AgentRepository;
import com.cms.repository.CommissionPayoutRepository;
import com.cms.repository.EnquiryRepository;

@ExtendWith(MockitoExtension.class)
class AgentLifecycleStatusToggleTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private EnquiryRepository enquiryRepository;
    @Mock
    private AgentCommissionGuidelineRepository agentCommissionGuidelineRepository;
    @Mock
    private CommissionPayoutRepository commissionPayoutRepository;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(
            agentRepository,
            enquiryRepository,
            agentCommissionGuidelineRepository,
            commissionPayoutRepository
        );
    }

    @Test
    void shouldBlockDeactivationWhenEnquiryReferenceExists() {
        Agent agent = createEntity(1L, true);
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(enquiryRepository.existsByAgentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> agentService.updateStatus(1L, new ActiveStatusUpdateRequest(false, "retired")))
            .isInstanceOf(LifecycleConflictException.class)
            .satisfies(ex -> {
                LifecycleConflictException conflict = (LifecycleConflictException) ex;
                assertThat(conflict.getCode()).isEqualTo("ACTIVE_REFERENCE_EXISTS");
            });
    }

    @Test
    void shouldActivateAgentWhenNoDependencyConflict() {
        Agent agent = createEntity(1L, false);
        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = agentService.updateStatus(1L, new ActiveStatusUpdateRequest(true, "reopen"));

        assertThat(response.isActive()).isTrue();
    }

    private Agent createEntity(Long id, boolean isActive) {
        Agent entity = new Agent("John", "9999999999", "john@example.com", "Town", "Main", isActive);
        entity.setId(id);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}

