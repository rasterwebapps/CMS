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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.ExamResultRequest;
import com.cms.dto.ExamResultResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.ExamResultStatus;
import com.cms.service.ExamResultService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ExamResultController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExamResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExamResultService examResultService;

    @Test
    void shouldCreateExamResult() throws Exception {
        ExamResultRequest request = new ExamResultRequest(
            1L, 1L, new BigDecimal("85.50"), "A", ExamResultStatus.PUBLISHED
        );

        ExamResultResponse response = createExamResultResponse();

        when(examResultService.create(any(ExamResultRequest.class))).thenReturn(response);

        mockMvc.perform(post("/exam-results")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.examinationId").value(1))
            .andExpect(jsonPath("$.studentId").value(1))
            .andExpect(jsonPath("$.grade").value("A"));

        verify(examResultService).create(any(ExamResultRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenExaminationIdNull() throws Exception {
        String jsonRequest = """
            {
                "studentId": 1,
                "marksObtained": 85.50,
                "grade": "A",
                "status": "PUBLISHED"
            }
            """;

        mockMvc.perform(post("/exam-results")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindByExaminationId() throws Exception {
        ExamResultResponse response = createExamResultResponse();

        when(examResultService.findByExaminationId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/exam-results/examination/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].examinationId").value(1));

        verify(examResultService).findByExaminationId(1L);
    }

    @Test
    void shouldFindByStudentId() throws Exception {
        ExamResultResponse response = createExamResultResponse();

        when(examResultService.findByStudentId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/exam-results/student/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].studentId").value(1));

        verify(examResultService).findByStudentId(1L);
    }

    @Test
    void shouldFindById() throws Exception {
        ExamResultResponse response = createExamResultResponse();

        when(examResultService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/exam-results/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.grade").value("A"));

        verify(examResultService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundById() throws Exception {
        when(examResultService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Exam result not found with id: 999"));

        mockMvc.perform(get("/exam-results/999"))
            .andExpect(status().isNotFound());

        verify(examResultService).findById(999L);
    }

    @Test
    void shouldUpdateExamResult() throws Exception {
        ExamResultRequest request = new ExamResultRequest(
            1L, 1L, new BigDecimal("90.00"), "A+", ExamResultStatus.PUBLISHED
        );

        ExamResultResponse response = new ExamResultResponse(
            1L, 1L, "Midterm", 1L, "John Doe", "ROLL001",
            new BigDecimal("90.00"), "A+", ExamResultStatus.PUBLISHED, Instant.now(), Instant.now()
        );

        when(examResultService.update(eq(1L), any(ExamResultRequest.class))).thenReturn(response);

        mockMvc.perform(put("/exam-results/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.grade").value("A+"));

        verify(examResultService).update(eq(1L), any(ExamResultRequest.class));
    }

    @Test
    void shouldDeleteExamResult() throws Exception {
        doNothing().when(examResultService).delete(1L);

        mockMvc.perform(delete("/exam-results/1"))
            .andExpect(status().isNoContent());

        verify(examResultService).delete(1L);
    }

    private ExamResultResponse createExamResultResponse() {
        return new ExamResultResponse(
            1L, 1L, "Midterm", 1L, "John Doe", "ROLL001",
            new BigDecimal("85.50"), "A", ExamResultStatus.PUBLISHED, Instant.now(), Instant.now()
        );
    }
}
