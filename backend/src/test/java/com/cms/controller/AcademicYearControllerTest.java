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

import com.cms.dto.AcademicYearRequest;
import com.cms.dto.AcademicYearResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.AcademicYearService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AcademicYearController.class)
@AutoConfigureMockMvc(addFilters = false)
class AcademicYearControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AcademicYearService academicYearService;

    @Test
    void shouldCreateAcademicYear() throws Exception {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            true
        );

        Instant now = Instant.now();
        AcademicYearResponse response = new AcademicYearResponse(
            1L,
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            true,
            now,
            now
        );

        when(academicYearService.create(any(AcademicYearRequest.class))).thenReturn(response);

        mockMvc.perform(post("/academic-years")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("2024-2025"))
            .andExpect(jsonPath("$.startDate").value("2024-08-01"))
            .andExpect(jsonPath("$.endDate").value("2025-05-31"))
            .andExpect(jsonPath("$.isCurrent").value(true));

        verify(academicYearService).create(any(AcademicYearRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        AcademicYearRequest request = new AcademicYearRequest(
            "",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        mockMvc.perform(post("/academic-years")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenStartDateIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "2024-2025",
                "endDate": "2025-05-31",
                "isCurrent": false
            }
            """;

        mockMvc.perform(post("/academic-years")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEndDateIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "2024-2025",
                "startDate": "2024-08-01",
                "isCurrent": false
            }
            """;

        mockMvc.perform(post("/academic-years")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEndDateIsBeforeStartDate() throws Exception {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2025, 5, 31),
            LocalDate.of(2024, 8, 1),
            false
        );

        when(academicYearService.create(any(AcademicYearRequest.class)))
            .thenThrow(new IllegalArgumentException("End date must be after start date"));

        mockMvc.perform(post("/academic-years")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("End date must be after start date"));
    }

    @Test
    void shouldFindAllAcademicYears() throws Exception {
        Instant now = Instant.now();
        AcademicYearResponse ay1 = new AcademicYearResponse(
            1L, "2023-2024", LocalDate.of(2023, 8, 1), LocalDate.of(2024, 5, 31),
            false, now, now
        );
        AcademicYearResponse ay2 = new AcademicYearResponse(
            2L, "2024-2025", LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31),
            true, now, now
        );

        when(academicYearService.findAll()).thenReturn(List.of(ay1, ay2));

        mockMvc.perform(get("/academic-years"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("2023-2024"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("2024-2025"));

        verify(academicYearService).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoAcademicYears() throws Exception {
        when(academicYearService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/academic-years"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(academicYearService).findAll();
    }

    @Test
    void shouldFindAcademicYearById() throws Exception {
        Instant now = Instant.now();
        AcademicYearResponse response = new AcademicYearResponse(
            1L,
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            true,
            now,
            now
        );

        when(academicYearService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/academic-years/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("2024-2025"))
            .andExpect(jsonPath("$.isCurrent").value(true));

        verify(academicYearService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenAcademicYearNotExists() throws Exception {
        when(academicYearService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Academic year not found with id: 999"));

        mockMvc.perform(get("/academic-years/999"))
            .andExpect(status().isNotFound());

        verify(academicYearService).findById(999L);
    }

    @Test
    void shouldFindCurrentAcademicYear() throws Exception {
        Instant now = Instant.now();
        AcademicYearResponse response = new AcademicYearResponse(
            1L,
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            true,
            now,
            now
        );

        when(academicYearService.findCurrent()).thenReturn(response);

        mockMvc.perform(get("/academic-years/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.isCurrent").value(true));

        verify(academicYearService).findCurrent();
    }

    @Test
    void shouldReturnNotFoundWhenNoCurrentAcademicYear() throws Exception {
        when(academicYearService.findCurrent())
            .thenThrow(new ResourceNotFoundException("No current academic year found"));

        mockMvc.perform(get("/academic-years/current"))
            .andExpect(status().isNotFound());

        verify(academicYearService).findCurrent();
    }

    @Test
    void shouldUpdateAcademicYear() throws Exception {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025 Updated",
            LocalDate.of(2024, 9, 1),
            LocalDate.of(2025, 6, 30),
            true
        );

        Instant now = Instant.now();
        AcademicYearResponse response = new AcademicYearResponse(
            1L,
            "2024-2025 Updated",
            LocalDate.of(2024, 9, 1),
            LocalDate.of(2025, 6, 30),
            true,
            now,
            now
        );

        when(academicYearService.update(eq(1L), any(AcademicYearRequest.class))).thenReturn(response);

        mockMvc.perform(put("/academic-years/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("2024-2025 Updated"))
            .andExpect(jsonPath("$.startDate").value("2024-09-01"))
            .andExpect(jsonPath("$.endDate").value("2025-06-30"));

        verify(academicYearService).update(eq(1L), any(AcademicYearRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentAcademicYear() throws Exception {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        when(academicYearService.update(eq(999L), any(AcademicYearRequest.class)))
            .thenThrow(new ResourceNotFoundException("Academic year not found with id: 999"));

        mockMvc.perform(put("/academic-years/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(academicYearService).update(eq(999L), any(AcademicYearRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidData() throws Exception {
        AcademicYearRequest request = new AcademicYearRequest(
            "",
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2025, 5, 31),
            false
        );

        mockMvc.perform(put("/academic-years/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidDates() throws Exception {
        AcademicYearRequest request = new AcademicYearRequest(
            "2024-2025",
            LocalDate.of(2025, 5, 31),
            LocalDate.of(2024, 8, 1),
            false
        );

        when(academicYearService.update(eq(1L), any(AcademicYearRequest.class)))
            .thenThrow(new IllegalArgumentException("End date must be after start date"));

        mockMvc.perform(put("/academic-years/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("End date must be after start date"));
    }

    @Test
    void shouldDeleteAcademicYear() throws Exception {
        doNothing().when(academicYearService).delete(1L);

        mockMvc.perform(delete("/academic-years/1"))
            .andExpect(status().isNoContent());

        verify(academicYearService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentAcademicYear() throws Exception {
        doThrow(new ResourceNotFoundException("Academic year not found with id: 999"))
            .when(academicYearService).delete(999L);

        mockMvc.perform(delete("/academic-years/999"))
            .andExpect(status().isNotFound());

        verify(academicYearService).delete(999L);
    }
}
