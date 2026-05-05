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

import com.cms.dto.CommunityRequest;
import com.cms.dto.CommunityResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.CommunityService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = CommunityController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommunityService communityService;

    @Test
    void shouldCreateCommunity() throws Exception {
        CommunityRequest request = new CommunityRequest("Backward Class", "BC", "BC category", true);
        CommunityResponse response = createResponse(1L, "Backward Class", "BC");

        when(communityService.create(any(CommunityRequest.class))).thenReturn(response);

        mockMvc.perform(post("/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Backward Class"))
            .andExpect(jsonPath("$.code").value("BC"));

        verify(communityService).create(any(CommunityRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        CommunityRequest request = new CommunityRequest("", "BC", null, true);

        mockMvc.perform(post("/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCodeIsBlank() throws Exception {
        CommunityRequest request = new CommunityRequest("Backward Class", "", null, true);

        mockMvc.perform(post("/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAll() throws Exception {
        CommunityResponse response = createResponse(1L, "Backward Class", "BC");

        when(communityService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/communities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(communityService).findAll();
    }

    @Test
    void shouldFindActiveOnly() throws Exception {
        CommunityResponse response = createResponse(1L, "Backward Class", "BC");

        when(communityService.findActive()).thenReturn(List.of(response));

        mockMvc.perform(get("/communities").param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(communityService).findActive();
    }

    @Test
    void shouldFindById() throws Exception {
        CommunityResponse response = createResponse(1L, "Backward Class", "BC");

        when(communityService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/communities/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.code").value("BC"));

        verify(communityService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenCommunityNotExists() throws Exception {
        when(communityService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Community not found with id: 999"));

        mockMvc.perform(get("/communities/999"))
            .andExpect(status().isNotFound());

        verify(communityService).findById(999L);
    }

    @Test
    void shouldUpdateCommunity() throws Exception {
        CommunityRequest request = new CommunityRequest("Backward Class Updated", "BC", null, true);
        CommunityResponse response = createResponse(1L, "Backward Class Updated", "BC");

        when(communityService.update(eq(1L), any(CommunityRequest.class))).thenReturn(response);

        mockMvc.perform(put("/communities/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Backward Class Updated"));

        verify(communityService).update(eq(1L), any(CommunityRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentCommunity() throws Exception {
        CommunityRequest request = new CommunityRequest("Name", "CODE", null, null);

        when(communityService.update(eq(999L), any(CommunityRequest.class)))
            .thenThrow(new ResourceNotFoundException("Community not found with id: 999"));

        mockMvc.perform(put("/communities/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(communityService).update(eq(999L), any(CommunityRequest.class));
    }

    @Test
    void shouldDeleteCommunity() throws Exception {
        doNothing().when(communityService).delete(1L);

        mockMvc.perform(delete("/communities/1"))
            .andExpect(status().isNoContent());

        verify(communityService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentCommunity() throws Exception {
        doThrow(new ResourceNotFoundException("Community not found with id: 999"))
            .when(communityService).delete(999L);

        mockMvc.perform(delete("/communities/999"))
            .andExpect(status().isNotFound());

        verify(communityService).delete(999L);
    }

    private CommunityResponse createResponse(Long id, String name, String code) {
        Instant now = Instant.now();
        return new CommunityResponse(id, name, code, name + " description", true, now, now);
    }
}

