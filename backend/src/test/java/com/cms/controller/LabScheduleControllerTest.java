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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.LabScheduleRequest;
import com.cms.dto.LabScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.DayOfWeek;
import com.cms.service.LabScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = LabScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LabScheduleService labScheduleService;

    @Test
    void shouldCreateLabSchedule() throws Exception {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        LabScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(labScheduleService.create(any(LabScheduleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/lab-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.batchName").value("Batch-A"));

        verify(labScheduleService).create(any(LabScheduleRequest.class));
    }

    @Test
    void shouldFindAllLabSchedules() throws Exception {
        LabScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(labScheduleService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(labScheduleService).findAll();
    }

    @Test
    void shouldFindByLabId() throws Exception {
        LabScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(labScheduleService.findByLabId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules").param("labId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(labScheduleService).findByLabId(1L);
    }

    @Test
    void shouldFindByFacultyId() throws Exception {
        LabScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(labScheduleService.findByFacultyId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules").param("facultyId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(labScheduleService).findByFacultyId(1L);
    }

    @Test
    void shouldFindByBatchName() throws Exception {
        LabScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(labScheduleService.findByBatchName("Batch-A")).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules").param("batchName", "Batch-A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(labScheduleService).findByBatchName("Batch-A");
    }

    @Test
    void shouldFindByDayOfWeek() throws Exception {
        LabScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(labScheduleService.findByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules").param("dayOfWeek", "MONDAY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(labScheduleService).findByDayOfWeek(DayOfWeek.MONDAY);
    }

    @Test
    void shouldFindLabScheduleById() throws Exception {
        LabScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(labScheduleService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/lab-schedules/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.batchName").value("Batch-A"));

        verify(labScheduleService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenLabScheduleNotExists() throws Exception {
        when(labScheduleService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Lab schedule not found with id: 999"));

        mockMvc.perform(get("/lab-schedules/999"))
            .andExpect(status().isNotFound());

        verify(labScheduleService).findById(999L);
    }

    @Test
    void shouldCheckConflicts() throws Exception {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-A", DayOfWeek.MONDAY, 1L, true
        );

        ScheduleConflictResponse response = new ScheduleConflictResponse(
            false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        when(labScheduleService.checkConflicts(any(LabScheduleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/lab-schedules/check-conflicts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasConflict").value(false));

        verify(labScheduleService).checkConflicts(any(LabScheduleRequest.class));
    }

    @Test
    void shouldUpdateLabSchedule() throws Exception {
        LabScheduleRequest request = new LabScheduleRequest(
            1L, 1L, 1L, 1L, "Batch-B", DayOfWeek.TUESDAY, 1L, true
        );

        LabScheduleResponse response = createResponse(1L, "Batch-B", DayOfWeek.TUESDAY);

        when(labScheduleService.update(eq(1L), any(LabScheduleRequest.class))).thenReturn(response);

        mockMvc.perform(put("/lab-schedules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.batchName").value("Batch-B"));

        verify(labScheduleService).update(eq(1L), any(LabScheduleRequest.class));
    }

    @Test
    void shouldDeleteLabSchedule() throws Exception {
        doNothing().when(labScheduleService).delete(1L);

        mockMvc.perform(delete("/lab-schedules/1"))
            .andExpect(status().isNoContent());

        verify(labScheduleService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentLabSchedule() throws Exception {
        doThrow(new ResourceNotFoundException("Lab schedule not found with id: 999"))
            .when(labScheduleService).delete(999L);

        mockMvc.perform(delete("/lab-schedules/999"))
            .andExpect(status().isNotFound());

        verify(labScheduleService).delete(999L);
    }

    private LabScheduleResponse createResponse(Long id, String batchName, DayOfWeek dayOfWeek) {
        Instant now = Instant.now();
        return new LabScheduleResponse(
            id, 1L, "Lab 1", 1L, "Data Structures Lab", "CS201L",
            1L, "John Doe", 1L, "Slot 1",
            LocalTime.of(9, 0), LocalTime.of(10, 30),
            batchName, dayOfWeek, 1L, "Odd Semester 2024",
            true, now, now
        );
    }
}
