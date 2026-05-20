package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

import com.cms.dto.IndiaDistrictRequest;
import com.cms.dto.IndiaDistrictResponse;
import com.cms.dto.IndiaStateRequest;
import com.cms.dto.IndiaStateResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.IndiaLocationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = IndiaLocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class IndiaLocationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private IndiaLocationService service;

    private IndiaStateResponse stateResponse(Long id, String name, String code) {
        return new IndiaStateResponse(id, name, code, true, Instant.now(), Instant.now());
    }

    private IndiaDistrictResponse districtResponse(Long id, Long stateId, String stateName, String name) {
        return new IndiaDistrictResponse(id, stateId, stateName, name, true, Instant.now(), Instant.now());
    }

    // ─── State endpoints ─────────────────────────────────────────────────────

    @Test
    void shouldGetActiveStates() throws Exception {
        when(service.findActiveStates()).thenReturn(List.of(stateResponse(1L, "Tamil Nadu", "TN")));

        mockMvc.perform(get("/india/states"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Tamil Nadu"))
            .andExpect(jsonPath("$[0].code").value("TN"));
    }

    @Test
    void shouldGetAllStatesWhenActiveOnlyFalse() throws Exception {
        when(service.findAllStates()).thenReturn(List.of(stateResponse(1L, "Tamil Nadu", "TN")));

        mockMvc.perform(get("/india/states?activeOnly=false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Tamil Nadu"));
    }

    @Test
    void shouldGetStateById() throws Exception {
        when(service.findStateById(1L)).thenReturn(stateResponse(1L, "Tamil Nadu", "TN"));

        mockMvc.perform(get("/india/states/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturn404WhenStateNotFound() throws Exception {
        when(service.findStateById(99L)).thenThrow(new ResourceNotFoundException("State not found with id: 99"));

        mockMvc.perform(get("/india/states/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateState() throws Exception {
        IndiaStateRequest req = new IndiaStateRequest("Tamil Nadu", "TN", true);
        when(service.createState(any())).thenReturn(stateResponse(1L, "Tamil Nadu", "TN"));

        mockMvc.perform(post("/india/states")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Tamil Nadu"));
    }

    @Test
    void shouldReturn400WhenStateNameBlank() throws Exception {
        IndiaStateRequest req = new IndiaStateRequest("", "TN", true);

        mockMvc.perform(post("/india/states")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateState() throws Exception {
        IndiaStateRequest req = new IndiaStateRequest("Tamil Nadu", "TN", true);
        when(service.updateState(eq(1L), any())).thenReturn(stateResponse(1L, "Tamil Nadu", "TN"));

        mockMvc.perform(put("/india/states/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteState() throws Exception {
        doNothing().when(service).deleteState(1L);

        mockMvc.perform(delete("/india/states/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentState() throws Exception {
        doThrow(new ResourceNotFoundException("State not found with id: 99")).when(service).deleteState(99L);

        mockMvc.perform(delete("/india/states/99"))
            .andExpect(status().isNotFound());
    }

    // ─── District endpoints ───────────────────────────────────────────────────

    @Test
    void shouldGetActiveDistricts() throws Exception {
        when(service.findActiveDistrictsByState(1L))
            .thenReturn(List.of(districtResponse(1L, 1L, "Tamil Nadu", "Chennai")));

        mockMvc.perform(get("/india/states/1/districts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Chennai"));
    }

    @Test
    void shouldCreateDistrict() throws Exception {
        IndiaDistrictRequest req = new IndiaDistrictRequest(1L, "Chennai", true);
        when(service.createDistrict(eq(1L), any())).thenReturn(districtResponse(1L, 1L, "Tamil Nadu", "Chennai"));

        mockMvc.perform(post("/india/states/1/districts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Chennai"));
    }

    @Test
    void shouldUpdateDistrict() throws Exception {
        IndiaDistrictRequest req = new IndiaDistrictRequest(1L, "Chennai Updated", true);
        when(service.updateDistrict(eq(1L), any())).thenReturn(districtResponse(1L, 1L, "Tamil Nadu", "Chennai Updated"));

        mockMvc.perform(put("/india/districts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Chennai Updated"));
    }

    @Test
    void shouldDeleteDistrict() throws Exception {
        doNothing().when(service).deleteDistrict(1L);

        mockMvc.perform(delete("/india/districts/1"))
            .andExpect(status().isNoContent());
    }
}

