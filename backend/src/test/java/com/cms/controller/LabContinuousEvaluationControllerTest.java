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

import com.cms.dto.LabContinuousEvaluationRequest;
import com.cms.dto.LabContinuousEvaluationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.LabContinuousEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = LabContinuousEvaluationController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabContinuousEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LabContinuousEvaluationService labContinuousEvaluationService;

    @Test
    void shouldCreateEvaluation() throws Exception {
        LabContinuousEvaluationRequest request = new LabContinuousEvaluationRequest(
            1L, 1L, 8, 7, 9, 24, LocalDate.of(2024, 6, 1), "Dr. Smith"
        );

        LabContinuousEvaluationResponse response = createEvaluationResponse();

        when(labContinuousEvaluationService.create(any(LabContinuousEvaluationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/lab-evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.experimentId").value(1))
            .andExpect(jsonPath("$.studentId").value(1))
            .andExpect(jsonPath("$.totalMarks").value(24));

        verify(labContinuousEvaluationService).create(any(LabContinuousEvaluationRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenExperimentIdNull() throws Exception {
        String jsonRequest = """
            {
                "studentId": 1,
                "recordMarks": 8,
                "vivaMarks": 7,
                "performanceMarks": 9,
                "totalMarks": 24
            }
            """;

        mockMvc.perform(post("/lab-evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindByExperimentId() throws Exception {
        LabContinuousEvaluationResponse response = createEvaluationResponse();

        when(labContinuousEvaluationService.findByExperimentId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-evaluations/experiment/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].experimentId").value(1));

        verify(labContinuousEvaluationService).findByExperimentId(1L);
    }

    @Test
    void shouldFindByStudentId() throws Exception {
        LabContinuousEvaluationResponse response = createEvaluationResponse();

        when(labContinuousEvaluationService.findByStudentId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/lab-evaluations/student/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].studentId").value(1));

        verify(labContinuousEvaluationService).findByStudentId(1L);
    }

    @Test
    void shouldFindById() throws Exception {
        LabContinuousEvaluationResponse response = createEvaluationResponse();

        when(labContinuousEvaluationService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/lab-evaluations/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.totalMarks").value(24));

        verify(labContinuousEvaluationService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundById() throws Exception {
        when(labContinuousEvaluationService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Lab evaluation not found with id: 999"));

        mockMvc.perform(get("/lab-evaluations/999"))
            .andExpect(status().isNotFound());

        verify(labContinuousEvaluationService).findById(999L);
    }

    @Test
    void shouldUpdateEvaluation() throws Exception {
        LabContinuousEvaluationRequest request = new LabContinuousEvaluationRequest(
            1L, 1L, 9, 8, 10, 27, LocalDate.of(2024, 6, 15), "Dr. Jones"
        );

        LabContinuousEvaluationResponse response = new LabContinuousEvaluationResponse(
            1L, 1L, "Ohm's Law", 1L, "John Doe", "ROLL001",
            9, 8, 10, 27, LocalDate.of(2024, 6, 15), "Dr. Jones", Instant.now(), Instant.now()
        );

        when(labContinuousEvaluationService.update(eq(1L), any(LabContinuousEvaluationRequest.class)))
            .thenReturn(response);

        mockMvc.perform(put("/lab-evaluations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.totalMarks").value(27));

        verify(labContinuousEvaluationService).update(eq(1L), any(LabContinuousEvaluationRequest.class));
    }

    @Test
    void shouldDeleteEvaluation() throws Exception {
        doNothing().when(labContinuousEvaluationService).delete(1L);

        mockMvc.perform(delete("/lab-evaluations/1"))
            .andExpect(status().isNoContent());

        verify(labContinuousEvaluationService).delete(1L);
    }

    private LabContinuousEvaluationResponse createEvaluationResponse() {
        return new LabContinuousEvaluationResponse(
            1L, 1L, "Ohm's Law", 1L, "John Doe", "ROLL001",
            8, 7, 9, 24, LocalDate.of(2024, 6, 1), "Dr. Smith", Instant.now(), Instant.now()
        );
    }
}
