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
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.MaintenanceRequestDto;
import com.cms.dto.MaintenanceRequestResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.MaintenancePriority;
import com.cms.model.enums.MaintenanceStatus;
import com.cms.model.enums.MaintenanceType;
import com.cms.service.MaintenanceRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = MaintenanceRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class MaintenanceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MaintenanceRequestService maintenanceRequestService;

    @Test
    void shouldCreateMaintenanceRequest() throws Exception {
        MaintenanceRequestDto request = new MaintenanceRequestDto(
            1L, "Screen not working", "Monitor shows no display", MaintenanceType.CORRECTIVE,
            MaintenancePriority.HIGH, MaintenanceStatus.REQUESTED, null, LocalDate.now(),
            null, null, null, null, null, null
        );

        MaintenanceRequestResponse response = createResponse(1L, "Screen not working", MaintenanceStatus.REQUESTED);

        when(maintenanceRequestService.create(any(MaintenanceRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/maintenance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Screen not working"));

        verify(maintenanceRequestService).create(any(MaintenanceRequestDto.class));
    }

    @Test
    void shouldFindAllRequests() throws Exception {
        MaintenanceRequestResponse response = createResponse(1L, "Screen not working", MaintenanceStatus.REQUESTED);

        when(maintenanceRequestService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/maintenance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(maintenanceRequestService).findAll();
    }

    @Test
    void shouldFindByEquipmentId() throws Exception {
        MaintenanceRequestResponse response = createResponse(1L, "Screen not working", MaintenanceStatus.REQUESTED);

        when(maintenanceRequestService.findByEquipmentId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/maintenance").param("equipmentId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(maintenanceRequestService).findByEquipmentId(1L);
    }

    @Test
    void shouldFindByStatus() throws Exception {
        MaintenanceRequestResponse response = createResponse(1L, "Screen not working", MaintenanceStatus.REQUESTED);

        when(maintenanceRequestService.findByStatus(MaintenanceStatus.REQUESTED)).thenReturn(List.of(response));

        mockMvc.perform(get("/maintenance").param("status", "REQUESTED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(maintenanceRequestService).findByStatus(MaintenanceStatus.REQUESTED);
    }

    @Test
    void shouldFindByAssignedToId() throws Exception {
        MaintenanceRequestResponse response = createResponse(1L, "Screen not working", MaintenanceStatus.REQUESTED);

        when(maintenanceRequestService.findByAssignedToId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/maintenance").param("assignedToId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(maintenanceRequestService).findByAssignedToId(1L);
    }

    @Test
    void shouldFindPendingRequests() throws Exception {
        MaintenanceRequestResponse response = createResponse(1L, "Screen not working", MaintenanceStatus.REQUESTED);

        when(maintenanceRequestService.findPendingRequests()).thenReturn(List.of(response));

        mockMvc.perform(get("/maintenance").param("pendingOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(maintenanceRequestService).findPendingRequests();
    }

    @Test
    void shouldFindById() throws Exception {
        MaintenanceRequestResponse response = createResponse(1L, "Screen not working", MaintenanceStatus.REQUESTED);

        when(maintenanceRequestService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/maintenance/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(maintenanceRequestService).findById(1L);
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        when(maintenanceRequestService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Maintenance request not found with id: 999"));

        mockMvc.perform(get("/maintenance/999"))
            .andExpect(status().isNotFound());

        verify(maintenanceRequestService).findById(999L);
    }

    @Test
    void shouldUpdateMaintenanceRequest() throws Exception {
        MaintenanceRequestDto request = new MaintenanceRequestDto(
            1L, "Screen fixed", "Replaced cable", MaintenanceType.CORRECTIVE,
            MaintenancePriority.HIGH, MaintenanceStatus.COMPLETED, null, LocalDate.now(),
            null, LocalDate.now(), null, null, null, null
        );

        MaintenanceRequestResponse response = createResponse(1L, "Screen fixed", MaintenanceStatus.COMPLETED);

        when(maintenanceRequestService.update(eq(1L), any(MaintenanceRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/maintenance/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(maintenanceRequestService).update(eq(1L), any(MaintenanceRequestDto.class));
    }

    @Test
    void shouldDeleteMaintenanceRequest() throws Exception {
        doNothing().when(maintenanceRequestService).delete(1L);

        mockMvc.perform(delete("/maintenance/1"))
            .andExpect(status().isNoContent());

        verify(maintenanceRequestService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Maintenance request not found with id: 999"))
            .when(maintenanceRequestService).delete(999L);

        mockMvc.perform(delete("/maintenance/999"))
            .andExpect(status().isNotFound());

        verify(maintenanceRequestService).delete(999L);
    }

    private MaintenanceRequestResponse createResponse(Long id, String title, MaintenanceStatus status) {
        Instant now = Instant.now();
        return new MaintenanceRequestResponse(
            id, 1L, "Dell Computer", "ASSET001", 1L, "Lab 1",
            title, "Description", MaintenanceType.CORRECTIVE, MaintenancePriority.HIGH,
            status, null, null, LocalDate.now(), null, null, null, null,
            null, null, null, now, now
        );
    }
}
