package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class AgentCommissionGuidelineServiceTest {

    @Mock
    private AgentCommissionGuidelineRepository guidelineRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private ProgramRepository programRepository;

    private AgentCommissionGuidelineService guidelineService;

    private Agent testAgent;
    private Program testProgram;

    @BeforeEach
    void setUp() {
        guidelineService = new AgentCommissionGuidelineService(guidelineRepository, agentRepository, programRepository);

        testAgent = new Agent("John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", true);
        testAgent.setId(1L);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Tech CS");
    }

    @Test
    void shouldCreateGuideline() {
        AgentCommissionGuidelineRequest request = new AgentCommissionGuidelineRequest(
            1L, 1L, LocalityType.LOCAL, new BigDecimal("5000.00")
        );

        AgentCommissionGuideline saved = createGuideline(1L, testAgent, testProgram,
            LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(agentRepository.findById(1L)).thenReturn(Optional.of(testAgent));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(guidelineRepository.save(any(AgentCommissionGuideline.class))).thenReturn(saved);

        AgentCommissionGuidelineResponse response = guidelineService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.localityType()).isEqualTo(LocalityType.LOCAL);
        assertThat(response.suggestedCommission()).isEqualTo(new BigDecimal("5000.00"));
        verify(guidelineRepository).save(any(AgentCommissionGuideline.class));
    }

    @Test
    void shouldThrowWhenAgentNotFoundOnCreate() {
        AgentCommissionGuidelineRequest request = new AgentCommissionGuidelineRequest(
            999L, 1L, LocalityType.LOCAL, new BigDecimal("5000.00")
        );

        when(agentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guidelineService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent not found with id: 999");
    }

    @Test
    void shouldThrowWhenProgramNotFoundOnCreate() {
        AgentCommissionGuidelineRequest request = new AgentCommissionGuidelineRequest(
            1L, 999L, LocalityType.LOCAL, new BigDecimal("5000.00")
        );

        when(agentRepository.findById(1L)).thenReturn(Optional.of(testAgent));
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guidelineService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldFindAllGuidelines() {
        AgentCommissionGuideline guideline = createGuideline(1L, testAgent, testProgram,
            LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineRepository.findAll()).thenReturn(List.of(guideline));

        List<AgentCommissionGuidelineResponse> responses = guidelineService.findAll();

        assertThat(responses).hasSize(1);
        verify(guidelineRepository).findAll();
    }

    @Test
    void shouldFindById() {
        AgentCommissionGuideline guideline = createGuideline(1L, testAgent, testProgram,
            LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineRepository.findById(1L)).thenReturn(Optional.of(guideline));

        AgentCommissionGuidelineResponse response = guidelineService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.agentName()).isEqualTo("John Agent");
        assertThat(response.programName()).isEqualTo("B.Tech CS");
        verify(guidelineRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(guidelineRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guidelineService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent commission guideline not found with id: 999");

        verify(guidelineRepository).findById(999L);
    }

    @Test
    void shouldFindByAgentId() {
        AgentCommissionGuideline guideline = createGuideline(1L, testAgent, testProgram,
            LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineRepository.findByAgentId(1L)).thenReturn(List.of(guideline));

        List<AgentCommissionGuidelineResponse> responses = guidelineService.findByAgentId(1L);

        assertThat(responses).hasSize(1);
        verify(guidelineRepository).findByAgentId(1L);
    }

    @Test
    void shouldFindByProgramId() {
        AgentCommissionGuideline guideline = createGuideline(1L, testAgent, testProgram,
            LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineRepository.findByProgramId(1L)).thenReturn(List.of(guideline));

        List<AgentCommissionGuidelineResponse> responses = guidelineService.findByProgramId(1L);

        assertThat(responses).hasSize(1);
        verify(guidelineRepository).findByProgramId(1L);
    }

    @Test
    void shouldFindGuideline() {
        AgentCommissionGuideline guideline = createGuideline(1L, testAgent, testProgram,
            LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineRepository.findByAgentIdAndProgramIdAndLocalityType(1L, 1L, LocalityType.LOCAL))
            .thenReturn(Optional.of(guideline));

        Optional<AgentCommissionGuidelineResponse> response = guidelineService.findGuideline(1L, 1L, LocalityType.LOCAL);

        assertThat(response).isPresent();
        assertThat(response.get().suggestedCommission()).isEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    void shouldReturnEmptyWhenGuidelineNotFound() {
        when(guidelineRepository.findByAgentIdAndProgramIdAndLocalityType(1L, 1L, LocalityType.STATE))
            .thenReturn(Optional.empty());

        Optional<AgentCommissionGuidelineResponse> response = guidelineService.findGuideline(1L, 1L, LocalityType.STATE);

        assertThat(response).isEmpty();
    }

    @Test
    void shouldUpdateGuideline() {
        AgentCommissionGuideline existing = createGuideline(1L, testAgent, testProgram,
            LocalityType.LOCAL, new BigDecimal("5000.00"));

        AgentCommissionGuidelineRequest updateRequest = new AgentCommissionGuidelineRequest(
            1L, 1L, LocalityType.DISTRICT, new BigDecimal("8000.00")
        );

        AgentCommissionGuideline updated = createGuideline(1L, testAgent, testProgram,
            LocalityType.DISTRICT, new BigDecimal("8000.00"));

        when(guidelineRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(agentRepository.findById(1L)).thenReturn(Optional.of(testAgent));
        when(programRepository.findById(1L)).thenReturn(Optional.of(testProgram));
        when(guidelineRepository.save(any(AgentCommissionGuideline.class))).thenReturn(updated);

        AgentCommissionGuidelineResponse response = guidelineService.update(1L, updateRequest);

        assertThat(response.localityType()).isEqualTo(LocalityType.DISTRICT);
        assertThat(response.suggestedCommission()).isEqualTo(new BigDecimal("8000.00"));
    }

    @Test
    void shouldThrowWhenNotFoundOnUpdate() {
        AgentCommissionGuidelineRequest request = new AgentCommissionGuidelineRequest(
            1L, 1L, LocalityType.LOCAL, new BigDecimal("5000.00")
        );

        when(guidelineRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guidelineService.update(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent commission guideline not found with id: 999");

        verify(guidelineRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAgentNotFoundOnUpdate() {
        AgentCommissionGuideline existing = createGuideline(1L, testAgent, testProgram,
            LocalityType.LOCAL, new BigDecimal("5000.00"));

        AgentCommissionGuidelineRequest request = new AgentCommissionGuidelineRequest(
            999L, 1L, LocalityType.LOCAL, new BigDecimal("5000.00")
        );

        when(guidelineRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(agentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guidelineService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent not found with id: 999");
    }

    @Test
    void shouldThrowWhenProgramNotFoundOnUpdate() {
        AgentCommissionGuideline existing = createGuideline(1L, testAgent, testProgram,
            LocalityType.LOCAL, new BigDecimal("5000.00"));

        AgentCommissionGuidelineRequest request = new AgentCommissionGuidelineRequest(
            1L, 999L, LocalityType.LOCAL, new BigDecimal("5000.00")
        );

        when(guidelineRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(agentRepository.findById(1L)).thenReturn(Optional.of(testAgent));
        when(programRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guidelineService.update(1L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Program not found with id: 999");
    }

    @Test
    void shouldDeleteGuideline() {
        when(guidelineRepository.existsById(1L)).thenReturn(true);

        guidelineService.delete(1L);

        verify(guidelineRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistent() {
        when(guidelineRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> guidelineService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Agent commission guideline not found with id: 999");

        verify(guidelineRepository, never()).deleteById(any());
    }

    private AgentCommissionGuideline createGuideline(Long id, Agent agent, Program program,
                                                      LocalityType localityType, BigDecimal suggestedCommission) {
        AgentCommissionGuideline guideline = new AgentCommissionGuideline(agent, program, localityType, suggestedCommission);
        guideline.setId(id);
        Instant now = Instant.now();
        guideline.setCreatedAt(now);
        guideline.setUpdatedAt(now);
        return guideline;
    }
}
