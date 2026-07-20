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

import com.cms.dto.ClassScheduleRequest;
import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.ScheduleConflictResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.service.ClassScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ClassScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClassScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClassScheduleService classScheduleService;

    private ClassScheduleRequest labRequest(String batchName, DayOfWeek dayOfWeek) {
        return new ClassScheduleRequest(
            ClassSessionType.LAB, 1L, 1L, 1L, 1L, batchName, null,
            dayOfWeek, 1L, true, null, null, null
        );
    }

    @Test
    void shouldCreateClassSchedule() throws Exception {
        ClassScheduleRequest request = labRequest("Batch-A", DayOfWeek.MONDAY);
        ClassScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(classScheduleService.create(any(ClassScheduleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/lab-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.batchName").value("Batch-A"));

        verify(classScheduleService).create(any(ClassScheduleRequest.class));
    }

    @Test
    void shouldFindAllClassSchedules() throws Exception {
        ClassScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(classScheduleService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(classScheduleService).findAll();
    }

    @Test
    void shouldFindByLabId() throws Exception {
        ClassScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(classScheduleService.findByLabId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules").param("labId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(classScheduleService).findByLabId(1L);
    }

    @Test
    void shouldFindByFacultyId() throws Exception {
        ClassScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(classScheduleService.findByFacultyId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules").param("facultyId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(classScheduleService).findByFacultyId(1L);
    }

    @Test
    void shouldFindByBatchName() throws Exception {
        ClassScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(classScheduleService.findByBatchName("Batch-A")).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules").param("batchName", "Batch-A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(classScheduleService).findByBatchName("Batch-A");
    }

    @Test
    void shouldFindByDayOfWeek() throws Exception {
        ClassScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(classScheduleService.findByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-schedules").param("dayOfWeek", "MONDAY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(classScheduleService).findByDayOfWeek(DayOfWeek.MONDAY);
    }

    @Test
    void shouldFindClassScheduleById() throws Exception {
        ClassScheduleResponse response = createResponse(1L, "Batch-A", DayOfWeek.MONDAY);

        when(classScheduleService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/lab-schedules/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.batchName").value("Batch-A"));

        verify(classScheduleService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenClassScheduleNotExists() throws Exception {
        when(classScheduleService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Class schedule not found with id: 999"));

        mockMvc.perform(get("/lab-schedules/999"))
            .andExpect(status().isNotFound());

        verify(classScheduleService).findById(999L);
    }

    @Test
    void shouldCheckConflicts() throws Exception {
        ClassScheduleRequest request = labRequest("Batch-A", DayOfWeek.MONDAY);

        ScheduleConflictResponse response = new ScheduleConflictResponse(
            false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        when(classScheduleService.checkConflicts(any(ClassScheduleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/lab-schedules/check-conflicts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasConflict").value(false));

        verify(classScheduleService).checkConflicts(any(ClassScheduleRequest.class));
    }

    @Test
    void shouldUpdateClassSchedule() throws Exception {
        ClassScheduleRequest request = labRequest("Batch-B", DayOfWeek.TUESDAY);

        ClassScheduleResponse response = createResponse(1L, "Batch-B", DayOfWeek.TUESDAY);

        when(classScheduleService.update(eq(1L), any(ClassScheduleRequest.class))).thenReturn(response);

        mockMvc.perform(put("/lab-schedules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.batchName").value("Batch-B"));

        verify(classScheduleService).update(eq(1L), any(ClassScheduleRequest.class));
    }

    @Test
    void shouldDeleteClassSchedule() throws Exception {
        doNothing().when(classScheduleService).delete(1L);

        mockMvc.perform(delete("/lab-schedules/1"))
            .andExpect(status().isNoContent());

        verify(classScheduleService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentClassSchedule() throws Exception {
        doThrow(new ResourceNotFoundException("Class schedule not found with id: 999"))
            .when(classScheduleService).delete(999L);

        mockMvc.perform(delete("/lab-schedules/999"))
            .andExpect(status().isNotFound());

        verify(classScheduleService).delete(999L);
    }

    private ClassScheduleResponse createResponse(Long id, String batchName, DayOfWeek dayOfWeek) {
        Instant now = Instant.now();
        return new ClassScheduleResponse(
            id, ClassSessionType.LAB, ClassScheduleStatus.PUBLISHED,
            1L, "Lab 1", 1L, "Data Structures Lab", "CS201L",
            1L, "John Doe", 1L, null, "Slot 1",
            LocalTime.of(9, 0), LocalTime.of(10, 30),
            batchName, null, null, "Lab 1", null,
            dayOfWeek, 1L, "Odd Semester 2024",
            true, now, now
        );
    }
}
