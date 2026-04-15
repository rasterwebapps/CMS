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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.ExperimentRequest;
import com.cms.dto.ExperimentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.ExperimentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ExperimentController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExperimentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExperimentService experimentService;

    @Test
    void shouldCreateExperiment() throws Exception {
        ExperimentRequest request = new ExperimentRequest(
            1L, 1, "Stack Implementation",
            "Description", "Aim", "Apparatus",
            "Procedure", "Expected", "Learning", 120, true
        );

        Instant now = Instant.now();
        ExperimentResponse response = new ExperimentResponse(
            1L, 1L, "Data Structures Lab", "CS201L",
            1, "Stack Implementation", "Description",
            "Aim", "Apparatus", "Procedure", "Expected",
            "Learning", 120, true, now, now
        );

        when(experimentService.create(any(ExperimentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.experimentNumber").value(1))
            .andExpect(jsonPath("$.name").value("Stack Implementation"));

        verify(experimentService).create(any(ExperimentRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        ExperimentRequest request = new ExperimentRequest(
            1L, 1, "",
            "Description", "Aim", "Apparatus",
            "Procedure", "Expected", "Learning", 120, true
        );

        mockMvc.perform(post("/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllExperiments() throws Exception {
        Instant now = Instant.now();
        ExperimentResponse response = new ExperimentResponse(
            1L, 1L, "Data Structures Lab", "CS201L",
            1, "Stack Implementation", "Description",
            "Aim", "Apparatus", "Procedure", "Expected",
            "Learning", 120, true, now, now
        );

        when(experimentService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/experiments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(experimentService).findAll();
    }

    @Test
    void shouldFindExperimentsByCourseId() throws Exception {
        Instant now = Instant.now();
        ExperimentResponse response = new ExperimentResponse(
            1L, 1L, "Data Structures Lab", "CS201L",
            1, "Stack Implementation", "Description",
            "Aim", "Apparatus", "Procedure", "Expected",
            "Learning", 120, true, now, now
        );

        when(experimentService.findBySubjectId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/experiments").param("subjectId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].subjectId").value(1));

        verify(experimentService).findBySubjectId(1L);
    }

    @Test
    void shouldFindExperimentById() throws Exception {
        Instant now = Instant.now();
        ExperimentResponse response = new ExperimentResponse(
            1L, 1L, "Data Structures Lab", "CS201L",
            1, "Stack Implementation", "Description",
            "Aim", "Apparatus", "Procedure", "Expected",
            "Learning", 120, true, now, now
        );

        when(experimentService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/experiments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Stack Implementation"));

        verify(experimentService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenExperimentNotExists() throws Exception {
        when(experimentService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Experiment not found with id: 999"));

        mockMvc.perform(get("/experiments/999"))
            .andExpect(status().isNotFound());

        verify(experimentService).findById(999L);
    }

    @Test
    void shouldUpdateExperiment() throws Exception {
        ExperimentRequest request = new ExperimentRequest(
            1L, 1, "Updated Stack",
            "Updated", "Updated", "Updated",
            "Updated", "Updated", "Updated", 180, true
        );

        Instant now = Instant.now();
        ExperimentResponse response = new ExperimentResponse(
            1L, 1L, "Data Structures Lab", "CS201L",
            1, "Updated Stack", "Updated",
            "Updated", "Updated", "Updated", "Updated",
            "Updated", 180, true, now, now
        );

        when(experimentService.update(eq(1L), any(ExperimentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/experiments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Stack"));

        verify(experimentService).update(eq(1L), any(ExperimentRequest.class));
    }

    @Test
    void shouldDeleteExperiment() throws Exception {
        doNothing().when(experimentService).delete(1L);

        mockMvc.perform(delete("/experiments/1"))
            .andExpect(status().isNoContent());

        verify(experimentService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentExperiment() throws Exception {
        doThrow(new ResourceNotFoundException("Experiment not found with id: 999"))
            .when(experimentService).delete(999L);

        mockMvc.perform(delete("/experiments/999"))
            .andExpect(status().isNotFound());

        verify(experimentService).delete(999L);
    }
}
