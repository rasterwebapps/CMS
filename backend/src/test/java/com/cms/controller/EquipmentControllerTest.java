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
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.EquipmentRequest;
import com.cms.dto.EquipmentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.EquipmentCategory;
import com.cms.model.enums.EquipmentStatus;
import com.cms.service.EquipmentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = EquipmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class EquipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EquipmentService equipmentService;

    @Test
    void shouldCreateEquipment() throws Exception {
        EquipmentRequest request = new EquipmentRequest(
            "Dell Computer", "ASSET001", "SN123", EquipmentCategory.COMPUTER, 1L,
            "Dell", "Optiplex 7090", EquipmentStatus.AVAILABLE,
            LocalDate.now(), new BigDecimal("50000.00"), null, null, null
        );

        EquipmentResponse response = createResponse(1L, "Dell Computer", EquipmentCategory.COMPUTER, EquipmentStatus.AVAILABLE);

        when(equipmentService.create(any(EquipmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/equipment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Dell Computer"));

        verify(equipmentService).create(any(EquipmentRequest.class));
    }

    @Test
    void shouldFindAllEquipment() throws Exception {
        EquipmentResponse response = createResponse(1L, "Dell Computer", EquipmentCategory.COMPUTER, EquipmentStatus.AVAILABLE);

        when(equipmentService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/equipment"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(equipmentService).findAll();
    }

    @Test
    void shouldFindByLabId() throws Exception {
        EquipmentResponse response = createResponse(1L, "Dell Computer", EquipmentCategory.COMPUTER, EquipmentStatus.AVAILABLE);

        when(equipmentService.findByLabId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/equipment").param("labId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(equipmentService).findByLabId(1L);
    }

    @Test
    void shouldFindByStatus() throws Exception {
        EquipmentResponse response = createResponse(1L, "Dell Computer", EquipmentCategory.COMPUTER, EquipmentStatus.AVAILABLE);

        when(equipmentService.findByStatus(EquipmentStatus.AVAILABLE)).thenReturn(List.of(response));

        mockMvc.perform(get("/equipment").param("status", "AVAILABLE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(equipmentService).findByStatus(EquipmentStatus.AVAILABLE);
    }

    @Test
    void shouldFindById() throws Exception {
        EquipmentResponse response = createResponse(1L, "Dell Computer", EquipmentCategory.COMPUTER, EquipmentStatus.AVAILABLE);

        when(equipmentService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/equipment/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(equipmentService).findById(1L);
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        when(equipmentService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Equipment not found with id: 999"));

        mockMvc.perform(get("/equipment/999"))
            .andExpect(status().isNotFound());

        verify(equipmentService).findById(999L);
    }

    @Test
    void shouldFindByAssetCode() throws Exception {
        EquipmentResponse response = createResponse(1L, "Dell Computer", EquipmentCategory.COMPUTER, EquipmentStatus.AVAILABLE);

        when(equipmentService.findByAssetCode("ASSET001")).thenReturn(response);

        mockMvc.perform(get("/equipment/asset/ASSET001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(equipmentService).findByAssetCode("ASSET001");
    }

    @Test
    void shouldUpdateEquipment() throws Exception {
        EquipmentRequest request = new EquipmentRequest(
            "HP Computer", "ASSET001", "SN123", EquipmentCategory.COMPUTER, 1L,
            "HP", "EliteDesk", EquipmentStatus.IN_USE, null, null, null, null, null
        );

        EquipmentResponse response = createResponse(1L, "HP Computer", EquipmentCategory.COMPUTER, EquipmentStatus.IN_USE);

        when(equipmentService.update(eq(1L), any(EquipmentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/equipment/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("HP Computer"));

        verify(equipmentService).update(eq(1L), any(EquipmentRequest.class));
    }

    @Test
    void shouldDeleteEquipment() throws Exception {
        doNothing().when(equipmentService).delete(1L);

        mockMvc.perform(delete("/equipment/1"))
            .andExpect(status().isNoContent());

        verify(equipmentService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Equipment not found with id: 999"))
            .when(equipmentService).delete(999L);

        mockMvc.perform(delete("/equipment/999"))
            .andExpect(status().isNotFound());

        verify(equipmentService).delete(999L);
    }

    private EquipmentResponse createResponse(Long id, String name, EquipmentCategory category, EquipmentStatus status) {
        Instant now = Instant.now();
        return new EquipmentResponse(
            id, name, "ASSET001", "SN123", category, 1L, "Lab 1",
            "Dell", "Optiplex", status, LocalDate.now(), new BigDecimal("50000.00"),
            null, null, null, now, now
        );
    }
}
