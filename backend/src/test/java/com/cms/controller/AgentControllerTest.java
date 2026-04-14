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

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.AgentRequest;
import com.cms.dto.AgentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AgentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AgentService agentService;

    @Test
    void shouldCreateAgent() throws Exception {
        AgentRequest request = new AgentRequest(
            "John Agent", "9876543210", "john@agent.com", "Salem", "Local Area", true
        );

        AgentResponse response = createResponse(1L, "John Agent", "9876543210",
            "john@agent.com", "Salem", "Local Area", true);

        when(agentService.create(any(AgentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("John Agent"));

        verify(agentService).create(any(AgentRequest.class));
    }

    @Test
    void shouldFindAllAgents() throws Exception {
        AgentResponse response = createResponse(1L, "John Agent", "9876543210",
            "john@agent.com", "Salem", "Local Area", true);

        when(agentService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/agents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(agentService).findAll();
    }

    @Test
    void shouldFindActiveAgents() throws Exception {
        AgentResponse response = createResponse(1L, "John Agent", "9876543210",
            "john@agent.com", "Salem", "Local Area", true);

        when(agentService.findActiveAgents()).thenReturn(List.of(response));

        mockMvc.perform(get("/agents").param("active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(agentService).findActiveAgents();
    }

    @Test
    void shouldFindById() throws Exception {
        AgentResponse response = createResponse(1L, "John Agent", "9876543210",
            "john@agent.com", "Salem", "Local Area", true);

        when(agentService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/agents/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("John Agent"));

        verify(agentService).findById(1L);
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        when(agentService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Agent not found with id: 999"));

        mockMvc.perform(get("/agents/999"))
            .andExpect(status().isNotFound());

        verify(agentService).findById(999L);
    }

    @Test
    void shouldUpdateAgent() throws Exception {
        AgentRequest request = new AgentRequest(
            "Jane Agent", "1234567890", "jane@agent.com", "Chennai", "City Area", false
        );

        AgentResponse response = createResponse(1L, "Jane Agent", "1234567890",
            "jane@agent.com", "Chennai", "City Area", false);

        when(agentService.update(eq(1L), any(AgentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/agents/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Jane Agent"));

        verify(agentService).update(eq(1L), any(AgentRequest.class));
    }

    @Test
    void shouldDeleteAgent() throws Exception {
        doNothing().when(agentService).delete(1L);

        mockMvc.perform(delete("/agents/1"))
            .andExpect(status().isNoContent());

        verify(agentService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Agent not found with id: 999"))
            .when(agentService).delete(999L);

        mockMvc.perform(delete("/agents/999"))
            .andExpect(status().isNotFound());

        verify(agentService).delete(999L);
    }

    private AgentResponse createResponse(Long id, String name, String phone, String email,
                                          String area, String locality, Boolean isActive) {
        Instant now = Instant.now();
        return new AgentResponse(id, name, phone, email, area, locality, isActive, now, now);
    }
}
