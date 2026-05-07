package com.cms.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.AcademicYearResponse;
import com.cms.dto.SemesterResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.SemesterStatus;
import com.cms.service.SemesterService;

@WebMvcTest(controllers = SemesterController.class)
@AutoConfigureMockMvc(addFilters = false)
class SemesterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SemesterService semesterService;

    @Test
    void shouldFindAllSemesters() throws Exception {
        Instant now = Instant.now();
        AcademicYearResponse ayResponse = new AcademicYearResponse(
            1L, "2024-2025", LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31),
            true, now, now
        );
        SemesterResponse sem1 = new SemesterResponse(
            1L, "Fall 2024", ayResponse, LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15), 1, SemesterStatus.UPCOMING, now, now
        );
        SemesterResponse sem2 = new SemesterResponse(
            2L, "Spring 2025", ayResponse, LocalDate.of(2025, 1, 15),
            LocalDate.of(2025, 5, 31), 2, SemesterStatus.UPCOMING, now, now
        );

        when(semesterService.findAll()).thenReturn(List.of(sem1, sem2));

        mockMvc.perform(get("/semesters"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Fall 2024"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("Spring 2025"));

        verify(semesterService).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoSemesters() throws Exception {
        when(semesterService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/semesters"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(semesterService).findAll();
    }

    @Test
    void shouldFindSemesterById() throws Exception {
        Instant now = Instant.now();
        AcademicYearResponse ayResponse = new AcademicYearResponse(
            1L, "2024-2025", LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31),
            true, now, now
        );
        SemesterResponse response = new SemesterResponse(
            1L, "Fall 2024", ayResponse, LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15), 1, SemesterStatus.UPCOMING, now, now
        );

        when(semesterService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/semesters/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Fall 2024"))
            .andExpect(jsonPath("$.semesterNumber").value(1));

        verify(semesterService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenSemesterNotExists() throws Exception {
        when(semesterService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Semester not found with id: 999"));

        mockMvc.perform(get("/semesters/999"))
            .andExpect(status().isNotFound());

        verify(semesterService).findById(999L);
    }

    @Test
    void shouldFindSemestersByAcademicYearId() throws Exception {
        Instant now = Instant.now();
        AcademicYearResponse ayResponse = new AcademicYearResponse(
            1L, "2024-2025", LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31),
            true, now, now
        );
        SemesterResponse sem1 = new SemesterResponse(
            1L, "Fall 2024", ayResponse, LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15), 1, SemesterStatus.UPCOMING, now, now
        );
        SemesterResponse sem2 = new SemesterResponse(
            2L, "Spring 2025", ayResponse, LocalDate.of(2025, 1, 15),
            LocalDate.of(2025, 5, 31), 2, SemesterStatus.UPCOMING, now, now
        );

        when(semesterService.findByAcademicYearId(1L)).thenReturn(List.of(sem1, sem2));

        mockMvc.perform(get("/semesters/academic-year/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].semesterNumber").value(1))
            .andExpect(jsonPath("$[1].semesterNumber").value(2));

        verify(semesterService).findByAcademicYearId(1L);
    }

    @Test
    void shouldReturnNotFoundWhenFindingSemestersByNonExistentAcademicYear() throws Exception {
        when(semesterService.findByAcademicYearId(999L))
            .thenThrow(new ResourceNotFoundException("Academic year not found with id: 999"));

        mockMvc.perform(get("/semesters/academic-year/999"))
            .andExpect(status().isNotFound());

        verify(semesterService).findByAcademicYearId(999L);
    }
}
