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

import com.cms.dto.AcademicYearResponse;
import com.cms.dto.SemesterRequest;
import com.cms.dto.SemesterResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.SemesterStatus;
import com.cms.service.SemesterService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SemesterController.class)
@AutoConfigureMockMvc(addFilters = false)
class SemesterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SemesterService semesterService;

    @Test
    void shouldCreateSemester() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        Instant now = Instant.now();
        AcademicYearResponse ayResponse = new AcademicYearResponse(
            1L, "2024-2025", LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31),
            true, now, now
        );
        SemesterResponse response = new SemesterResponse(
            1L,
            "Fall 2024",
            ayResponse,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1,
            SemesterStatus.UPCOMING,
            now,
            now
        );

        when(semesterService.create(any(SemesterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Fall 2024"))
            .andExpect(jsonPath("$.semesterNumber").value(1))
            .andExpect(jsonPath("$.startDate").value("2024-08-01"))
            .andExpect(jsonPath("$.endDate").value("2024-12-15"))
            .andExpect(jsonPath("$.academicYear.id").value(1));

        verify(semesterService).create(any(SemesterRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenAcademicYearIdIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Fall 2024",
                "startDate": "2024-08-01",
                "endDate": "2024-12-15",
                "semesterNumber": 1
            }
            """;

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenStartDateIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Fall 2024",
                "academicYearId": 1,
                "endDate": "2024-12-15",
                "semesterNumber": 1
            }
            """;

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEndDateIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Fall 2024",
                "academicYearId": 1,
                "startDate": "2024-08-01",
                "semesterNumber": 1
            }
            """;

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSemesterNumberIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Fall 2024",
                "academicYearId": 1,
                "startDate": "2024-08-01",
                "endDate": "2024-12-15"
            }
            """;

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSemesterNumberTooLow() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            0
        );

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSemesterNumberTooHigh() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            9
        );

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEndDateIsBeforeStartDate() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 12, 15),
            LocalDate.of(2024, 8, 1),
            1
        );

        when(semesterService.create(any(SemesterRequest.class)))
            .thenThrow(new IllegalArgumentException("End date must be after start date"));

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("End date must be after start date"));
    }

    @Test
    void shouldReturnBadRequestWhenSemesterStartDateBeforeAcademicYear() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        when(semesterService.create(any(SemesterRequest.class)))
            .thenThrow(new IllegalArgumentException(
                "Semester start date must not be before academic year start date"));

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Semester start date must not be before academic year start date"));
    }

    @Test
    void shouldReturnBadRequestWhenSemesterEndDateAfterAcademicYear() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "Spring 2025",
            1L,
            LocalDate.of(2025, 1, 15),
            LocalDate.of(2025, 6, 30),
            2
        );

        when(semesterService.create(any(SemesterRequest.class)))
            .thenThrow(new IllegalArgumentException(
                "Semester end date must not be after academic year end date"));

        mockMvc.perform(post("/semesters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Semester end date must not be after academic year end date"));
    }

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
            1L,
            "Fall 2024",
            ayResponse,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1,
            SemesterStatus.UPCOMING,
            now,
            now
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
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].semesterNumber").value(1))
            .andExpect(jsonPath("$[1].id").value(2))
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

    @Test
    void shouldUpdateSemester() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024 Updated",
            1L,
            LocalDate.of(2024, 8, 15),
            LocalDate.of(2024, 12, 20),
            1
        );

        Instant now = Instant.now();
        AcademicYearResponse ayResponse = new AcademicYearResponse(
            1L, "2024-2025", LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31),
            true, now, now
        );
        SemesterResponse response = new SemesterResponse(
            1L,
            "Fall 2024 Updated",
            ayResponse,
            LocalDate.of(2024, 8, 15),
            LocalDate.of(2024, 12, 20),
            1,
            SemesterStatus.UPCOMING,
            now,
            now
        );

        when(semesterService.update(eq(1L), any(SemesterRequest.class))).thenReturn(response);

        mockMvc.perform(put("/semesters/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Fall 2024 Updated"))
            .andExpect(jsonPath("$.startDate").value("2024-08-15"))
            .andExpect(jsonPath("$.endDate").value("2024-12-20"));

        verify(semesterService).update(eq(1L), any(SemesterRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentSemester() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        when(semesterService.update(eq(999L), any(SemesterRequest.class)))
            .thenThrow(new ResourceNotFoundException("Semester not found with id: 999"));

        mockMvc.perform(put("/semesters/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(semesterService).update(eq(999L), any(SemesterRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidData() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "",
            1L,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 12, 15),
            1
        );

        mockMvc.perform(put("/semesters/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidDates() throws Exception {
        SemesterRequest request = new SemesterRequest(
            "Fall 2024",
            1L,
            LocalDate.of(2024, 12, 15),
            LocalDate.of(2024, 8, 1),
            1
        );

        when(semesterService.update(eq(1L), any(SemesterRequest.class)))
            .thenThrow(new IllegalArgumentException("End date must be after start date"));

        mockMvc.perform(put("/semesters/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("End date must be after start date"));
    }

    @Test
    void shouldDeleteSemester() throws Exception {
        doNothing().when(semesterService).delete(1L);

        mockMvc.perform(delete("/semesters/1"))
            .andExpect(status().isNoContent());

        verify(semesterService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentSemester() throws Exception {
        doThrow(new ResourceNotFoundException("Semester not found with id: 999"))
            .when(semesterService).delete(999L);

        mockMvc.perform(delete("/semesters/999"))
            .andExpect(status().isNotFound());

        verify(semesterService).delete(999L);
    }
}
