package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.AgentRequest;
import com.cms.dto.AgentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Agent;
import com.cms.repository.AgentRepository;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(agentRepository);
    }

    @Test
    void shouldCreateAgent() {
        AgentRequest request = new AgentRequest(
            "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", 50, true
        );

        Agent saved = createAgent(1L, "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", 50, true);

        when(agentRepository.save(any(Agent.class))).thenReturn(saved);

        AgentResponse response = agentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("John Agent");
        assertThat(response.phone()).isEqualTo("9876543210");
        assertThat(response.allottedSeats()).isEqualTo(50);
        assertThat(response.isActive()).isTrue();
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    void shouldCreateAgentWithDefaultIsActive() {
        AgentRequest request = new AgentRequest(
            "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", null, null
        );

        Agent saved = createAgent(1L, "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", null, true);

        when(agentRepository.save(any(Agent.class))).thenReturn(saved);

        AgentResponse response = agentService.create(request);

        assertThat(response.isActive()).isTrue();
    }

    @Test
    void shouldFindAllAgents() {
        Agent agent = createAgent(1L, "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", null, true);

        when(agentRepository.findAll()).thenReturn(List.of(agent));

        List<AgentResponse> responses = agentService.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("John Agent");
        verify(agentRepository).findAll();
    }

    @Test
    void shouldFindById() {
        Agent agent = createAgent(1L, "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", null, true);

        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));

        AgentResponse response = agentService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("John Agent");
        verify(agentRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(agentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent not found with id: 999");

        verify(agentRepository).findById(999L);
    }

    @Test
    void shouldFindActiveAgents() {
        Agent agent = createAgent(1L, "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", null, true);

        when(agentRepository.findByIsActiveTrue()).thenReturn(List.of(agent));

        List<AgentResponse> responses = agentService.findActiveAgents();

        assertThat(responses).hasSize(1);
        verify(agentRepository).findByIsActiveTrue();
    }

    @Test
    void shouldUpdateAgent() {
        Agent existing = createAgent(1L, "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", 50, true);

        AgentRequest updateRequest = new AgentRequest(
            "Jane Agent", "1234567890", "jane@agent.com", "Chennai", "City Area", 100, false
        );

        Agent updated = createAgent(1L, "Jane Agent", "1234567890", "jane@agent.com", "Chennai", "City Area", 100, false);

        when(agentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(agentRepository.existsByNameAndIdNot("Jane Agent", 1L)).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenReturn(updated);

        AgentResponse response = agentService.update(1L, updateRequest);

        assertThat(response.name()).isEqualTo("Jane Agent");
        assertThat(response.allottedSeats()).isEqualTo(100);
        assertThat(response.isActive()).isFalse();
        verify(agentRepository).findById(1L);
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithDuplicateName() {
        Agent existing = createAgent(1L, "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", 50, true);

        AgentRequest updateRequest = new AgentRequest(
            "Existing Agent", "1234567890", "jane@agent.com", "Chennai", "City Area", 100, true
        );

        when(agentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(agentRepository.existsByNameAndIdNot("Existing Agent", 1L)).thenReturn(true);

        assertThatThrownBy(() -> agentService.update(1L, updateRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("An agent with the name 'Existing Agent' already exists");

        verify(agentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNotFoundOnUpdate() {
        AgentRequest request = new AgentRequest(
            "Jane Agent", "1234567890", "jane@agent.com", "Chennai", "City Area", null, true
        );

        when(agentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent not found with id: 999");

        verify(agentRepository).findById(999L);
        verify(agentRepository, never()).save(any());
    }

    @Test
    void shouldDeleteAgent() {
        when(agentRepository.existsById(1L)).thenReturn(true);

        agentService.delete(1L);

        verify(agentRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(agentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> agentService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent not found with id: 999");

        verify(agentRepository, never()).deleteById(any());
    }

    private Agent createAgent(Long id, String name, String phone, String email,
                               String area, String locality, Integer allottedSeats, Boolean isActive) {
        Agent agent = new Agent(name, phone, email, area, locality, isActive);
        agent.setId(id);
        agent.setAllottedSeats(allottedSeats);
        Instant now = Instant.now();
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);
        return agent;
    }
}
