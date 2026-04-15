package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.AgentCommissionGuidelineRequest;
import com.cms.dto.AgentCommissionGuidelineResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.LocalityType;
import com.cms.service.AgentCommissionGuidelineService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AgentCommissionGuidelineController.class)
@AutoConfigureMockMvc(addFilters = false)
class AgentCommissionGuidelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AgentCommissionGuidelineService guidelineService;

    @Test
    void shouldCreateGuideline() throws Exception {
        AgentCommissionGuidelineRequest request = new AgentCommissionGuidelineRequest(
            1L, 1L, LocalityType.LOCAL, new BigDecimal("5000.00")
        );

        AgentCommissionGuidelineResponse response = createResponse(1L, LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineService.create(any(AgentCommissionGuidelineRequest.class))).thenReturn(response);

        mockMvc.perform(post("/agent-commission-guidelines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.localityType").value("LOCAL"));

        verify(guidelineService).create(any(AgentCommissionGuidelineRequest.class));
    }

    @Test
    void shouldFindAllGuidelines() throws Exception {
        AgentCommissionGuidelineResponse response = createResponse(1L, LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/agent-commission-guidelines"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(guidelineService).findAll();
    }

    @Test
    void shouldFindByAgentId() throws Exception {
        AgentCommissionGuidelineResponse response = createResponse(1L, LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineService.findByAgentId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/agent-commission-guidelines").param("agentId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(guidelineService).findByAgentId(1L);
    }

    @Test
    void shouldFindByProgramId() throws Exception {
        AgentCommissionGuidelineResponse response = createResponse(1L, LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineService.findByProgramId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/agent-commission-guidelines").param("programId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(guidelineService).findByProgramId(1L);
    }

    @Test
    void shouldFindById() throws Exception {
        AgentCommissionGuidelineResponse response = createResponse(1L, LocalityType.LOCAL, new BigDecimal("5000.00"));

        when(guidelineService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/agent-commission-guidelines/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(guidelineService).findById(1L);
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        when(guidelineService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Agent commission guideline not found with id: 999"));

        mockMvc.perform(get("/agent-commission-guidelines/999"))
            .andExpect(status().isNotFound());

        verify(guidelineService).findById(999L);
    }

    @Test
    void shouldUpdateGuideline() throws Exception {
        AgentCommissionGuidelineRequest request = new AgentCommissionGuidelineRequest(
            1L, 1L, LocalityType.DISTRICT, new BigDecimal("8000.00")
        );

        AgentCommissionGuidelineResponse response = createResponse(1L, LocalityType.DISTRICT, new BigDecimal("8000.00"));

        when(guidelineService.update(eq(1L), any(AgentCommissionGuidelineRequest.class))).thenReturn(response);

        mockMvc.perform(put("/agent-commission-guidelines/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.localityType").value("DISTRICT"));

        verify(guidelineService).update(eq(1L), any(AgentCommissionGuidelineRequest.class));
    }

    @Test
    void shouldDeleteGuideline() throws Exception {
        doNothing().when(guidelineService).delete(1L);

        mockMvc.perform(delete("/agent-commission-guidelines/1"))
            .andExpect(status().isNoContent());

        verify(guidelineService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Agent commission guideline not found with id: 999"))
            .when(guidelineService).delete(999L);

        mockMvc.perform(delete("/agent-commission-guidelines/999"))
            .andExpect(status().isNotFound());

        verify(guidelineService).delete(999L);
    }

    private AgentCommissionGuidelineResponse createResponse(Long id, LocalityType localityType,
                                                             BigDecimal suggestedCommission) {
        Instant now = Instant.now();
        return new AgentCommissionGuidelineResponse(
            id, 1L, "John Agent", 1L, "B.Tech CS", localityType, suggestedCommission, now, now
        );
    }
}
