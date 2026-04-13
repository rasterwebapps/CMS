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
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.LabSlotRequest;
import com.cms.dto.LabSlotResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.LabSlotService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = LabSlotController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LabSlotService labSlotService;

    @Test
    void shouldCreateLabSlot() throws Exception {
        LabSlotRequest request = new LabSlotRequest(
            "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true
        );

        Instant now = Instant.now();
        LabSlotResponse response = new LabSlotResponse(
            1L, "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true, now, now
        );

        when(labSlotService.create(any(LabSlotRequest.class))).thenReturn(response);

        mockMvc.perform(post("/lab-slots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Slot 1"));

        verify(labSlotService).create(any(LabSlotRequest.class));
    }

    @Test
    void shouldFindAllLabSlots() throws Exception {
        Instant now = Instant.now();
        LabSlotResponse response = new LabSlotResponse(
            1L, "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true, now, now
        );

        when(labSlotService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-slots"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(labSlotService).findAll();
    }

    @Test
    void shouldFindActiveLabSlots() throws Exception {
        Instant now = Instant.now();
        LabSlotResponse response = new LabSlotResponse(
            1L, "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true, now, now
        );

        when(labSlotService.findAllActive()).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-slots").param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(labSlotService).findAllActive();
    }

    @Test
    void shouldFindLabSlotById() throws Exception {
        Instant now = Instant.now();
        LabSlotResponse response = new LabSlotResponse(
            1L, "Slot 1", LocalTime.of(9, 0), LocalTime.of(10, 30), 1, true, now, now
        );

        when(labSlotService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/lab-slots/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Slot 1"));

        verify(labSlotService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenLabSlotNotExists() throws Exception {
        when(labSlotService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Lab slot not found with id: 999"));

        mockMvc.perform(get("/lab-slots/999"))
            .andExpect(status().isNotFound());

        verify(labSlotService).findById(999L);
    }

    @Test
    void shouldUpdateLabSlot() throws Exception {
        LabSlotRequest request = new LabSlotRequest(
            "Updated Slot", LocalTime.of(8, 30), LocalTime.of(10, 0), 1, true
        );

        Instant now = Instant.now();
        LabSlotResponse response = new LabSlotResponse(
            1L, "Updated Slot", LocalTime.of(8, 30), LocalTime.of(10, 0), 1, true, now, now
        );

        when(labSlotService.update(eq(1L), any(LabSlotRequest.class))).thenReturn(response);

        mockMvc.perform(put("/lab-slots/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Slot"));

        verify(labSlotService).update(eq(1L), any(LabSlotRequest.class));
    }

    @Test
    void shouldDeleteLabSlot() throws Exception {
        doNothing().when(labSlotService).delete(1L);

        mockMvc.perform(delete("/lab-slots/1"))
            .andExpect(status().isNoContent());

        verify(labSlotService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentLabSlot() throws Exception {
        doThrow(new ResourceNotFoundException("Lab slot not found with id: 999"))
            .when(labSlotService).delete(999L);

        mockMvc.perform(delete("/lab-slots/999"))
            .andExpect(status().isNotFound());

        verify(labSlotService).delete(999L);
    }
}
