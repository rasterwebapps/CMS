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

import com.cms.dto.ExaminationRequest;
import com.cms.dto.ExaminationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.ExamType;
import com.cms.service.ExaminationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ExaminationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExaminationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExaminationService examinationService;

    @Test
    void shouldCreateExamination() throws Exception {
        ExaminationRequest request = new ExaminationRequest(
            "Midterm", 1L, ExamType.THEORY, LocalDate.of(2024, 6, 1), 120, 100, 1L
        );

        ExaminationResponse response = createExaminationResponse();

        when(examinationService.create(any(ExaminationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/examinations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Midterm"))
            .andExpect(jsonPath("$.examType").value("THEORY"));

        verify(examinationService).create(any(ExaminationRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        ExaminationRequest request = new ExaminationRequest(
            "", 1L, ExamType.THEORY, LocalDate.of(2024, 6, 1), 120, 100, 1L
        );

        mockMvc.perform(post("/examinations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllExaminations() throws Exception {
        ExaminationResponse response = createExaminationResponse();

        when(examinationService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/examinations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Midterm"));

        verify(examinationService).findAll();
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        when(examinationService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/examinations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(examinationService).findAll();
    }

    @Test
    void shouldFindById() throws Exception {
        ExaminationResponse response = createExaminationResponse();

        when(examinationService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/examinations/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Midterm"));

        verify(examinationService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundById() throws Exception {
        when(examinationService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Examination not found with id: 999"));

        mockMvc.perform(get("/examinations/999"))
            .andExpect(status().isNotFound());

        verify(examinationService).findById(999L);
    }

    @Test
    void shouldFindByCourseId() throws Exception {
        ExaminationResponse response = createExaminationResponse();

        when(examinationService.findByCourseId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/examinations/course/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].courseId").value(1));

        verify(examinationService).findByCourseId(1L);
    }

    @Test
    void shouldFindBySemesterId() throws Exception {
        ExaminationResponse response = createExaminationResponse();

        when(examinationService.findBySemesterId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/examinations/semester/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].semesterId").value(1));

        verify(examinationService).findBySemesterId(1L);
    }

    @Test
    void shouldUpdateExamination() throws Exception {
        ExaminationRequest request = new ExaminationRequest(
            "Midterm Updated", 1L, ExamType.PRACTICAL, LocalDate.of(2024, 7, 1), 90, 50, 1L
        );

        ExaminationResponse response = new ExaminationResponse(
            1L, "Midterm Updated", 1L, "Physics", ExamType.PRACTICAL,
            LocalDate.of(2024, 7, 1), 90, 50, 1L, "Semester 1", Instant.now(), Instant.now()
        );

        when(examinationService.update(eq(1L), any(ExaminationRequest.class))).thenReturn(response);

        mockMvc.perform(put("/examinations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Midterm Updated"))
            .andExpect(jsonPath("$.examType").value("PRACTICAL"));

        verify(examinationService).update(eq(1L), any(ExaminationRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdating() throws Exception {
        ExaminationRequest request = new ExaminationRequest(
            "Midterm", 1L, ExamType.THEORY, LocalDate.of(2024, 6, 1), 120, 100, 1L
        );

        when(examinationService.update(eq(999L), any(ExaminationRequest.class)))
            .thenThrow(new ResourceNotFoundException("Examination not found with id: 999"));

        mockMvc.perform(put("/examinations/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(examinationService).update(eq(999L), any(ExaminationRequest.class));
    }

    @Test
    void shouldDeleteExamination() throws Exception {
        doNothing().when(examinationService).delete(1L);

        mockMvc.perform(delete("/examinations/1"))
            .andExpect(status().isNoContent());

        verify(examinationService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Examination not found with id: 999"))
            .when(examinationService).delete(999L);

        mockMvc.perform(delete("/examinations/999"))
            .andExpect(status().isNotFound());

        verify(examinationService).delete(999L);
    }

    private ExaminationResponse createExaminationResponse() {
        return new ExaminationResponse(
            1L, "Midterm", 1L, "Physics", ExamType.THEORY,
            LocalDate.of(2024, 6, 1), 120, 100, 1L, "Semester 1", Instant.now(), Instant.now()
        );
    }
}
