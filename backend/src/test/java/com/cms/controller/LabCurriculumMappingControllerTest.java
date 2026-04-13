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

import com.cms.dto.LabCurriculumMappingRequest;
import com.cms.dto.LabCurriculumMappingResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.MappingLevel;
import com.cms.model.enums.OutcomeType;
import com.cms.service.LabCurriculumMappingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = LabCurriculumMappingController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabCurriculumMappingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LabCurriculumMappingService mappingService;

    @Test
    void shouldCreateMapping() throws Exception {
        LabCurriculumMappingRequest request = new LabCurriculumMappingRequest(
            1L, OutcomeType.COURSE_OUTCOME, "CO1",
            "Understand concepts", MappingLevel.HIGH, "Direct correlation"
        );

        Instant now = Instant.now();
        LabCurriculumMappingResponse response = new LabCurriculumMappingResponse(
            1L, 1L, "Stack Implementation", 1, 1L, "Data Structures Lab",
            OutcomeType.COURSE_OUTCOME, "CO1", "Understand concepts",
            MappingLevel.HIGH, "Direct correlation", now, now
        );

        when(mappingService.create(any(LabCurriculumMappingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/curriculum-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.outcomeType").value("COURSE_OUTCOME"))
            .andExpect(jsonPath("$.outcomeCode").value("CO1"))
            .andExpect(jsonPath("$.mappingLevel").value("HIGH"));

        verify(mappingService).create(any(LabCurriculumMappingRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenOutcomeCodeIsBlank() throws Exception {
        LabCurriculumMappingRequest request = new LabCurriculumMappingRequest(
            1L, OutcomeType.COURSE_OUTCOME, "",
            "Description", MappingLevel.HIGH, "Justification"
        );

        mockMvc.perform(post("/curriculum-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllMappings() throws Exception {
        Instant now = Instant.now();
        LabCurriculumMappingResponse response = new LabCurriculumMappingResponse(
            1L, 1L, "Stack Implementation", 1, 1L, "Data Structures Lab",
            OutcomeType.COURSE_OUTCOME, "CO1", "Understand concepts",
            MappingLevel.HIGH, "Direct correlation", now, now
        );

        when(mappingService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/curriculum-mappings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(mappingService).findAll();
    }

    @Test
    void shouldFindMappingsByExperimentId() throws Exception {
        Instant now = Instant.now();
        LabCurriculumMappingResponse response = new LabCurriculumMappingResponse(
            1L, 1L, "Stack Implementation", 1, 1L, "Data Structures Lab",
            OutcomeType.COURSE_OUTCOME, "CO1", "Understand concepts",
            MappingLevel.HIGH, "Direct correlation", now, now
        );

        when(mappingService.findByExperimentId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/curriculum-mappings").param("experimentId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].experimentId").value(1));

        verify(mappingService).findByExperimentId(1L);
    }

    @Test
    void shouldFindMappingsByExperimentIdAndOutcomeType() throws Exception {
        Instant now = Instant.now();
        LabCurriculumMappingResponse response = new LabCurriculumMappingResponse(
            1L, 1L, "Stack Implementation", 1, 1L, "Data Structures Lab",
            OutcomeType.COURSE_OUTCOME, "CO1", "Understand concepts",
            MappingLevel.HIGH, "Direct correlation", now, now
        );

        when(mappingService.findByExperimentIdAndOutcomeType(1L, OutcomeType.COURSE_OUTCOME))
            .thenReturn(List.of(response));

        mockMvc.perform(get("/curriculum-mappings")
                .param("experimentId", "1")
                .param("outcomeType", "COURSE_OUTCOME"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].outcomeType").value("COURSE_OUTCOME"));

        verify(mappingService).findByExperimentIdAndOutcomeType(1L, OutcomeType.COURSE_OUTCOME);
    }

    @Test
    void shouldFindMappingById() throws Exception {
        Instant now = Instant.now();
        LabCurriculumMappingResponse response = new LabCurriculumMappingResponse(
            1L, 1L, "Stack Implementation", 1, 1L, "Data Structures Lab",
            OutcomeType.COURSE_OUTCOME, "CO1", "Understand concepts",
            MappingLevel.HIGH, "Direct correlation", now, now
        );

        when(mappingService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/curriculum-mappings/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.outcomeCode").value("CO1"));

        verify(mappingService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenMappingNotExists() throws Exception {
        when(mappingService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Lab curriculum mapping not found with id: 999"));

        mockMvc.perform(get("/curriculum-mappings/999"))
            .andExpect(status().isNotFound());

        verify(mappingService).findById(999L);
    }

    @Test
    void shouldUpdateMapping() throws Exception {
        LabCurriculumMappingRequest request = new LabCurriculumMappingRequest(
            1L, OutcomeType.PROGRAM_OUTCOME, "PO1",
            "Updated desc", MappingLevel.MEDIUM, "Updated just"
        );

        Instant now = Instant.now();
        LabCurriculumMappingResponse response = new LabCurriculumMappingResponse(
            1L, 1L, "Stack Implementation", 1, 1L, "Data Structures Lab",
            OutcomeType.PROGRAM_OUTCOME, "PO1", "Updated desc",
            MappingLevel.MEDIUM, "Updated just", now, now
        );

        when(mappingService.update(eq(1L), any(LabCurriculumMappingRequest.class))).thenReturn(response);

        mockMvc.perform(put("/curriculum-mappings/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcomeType").value("PROGRAM_OUTCOME"))
            .andExpect(jsonPath("$.outcomeCode").value("PO1"));

        verify(mappingService).update(eq(1L), any(LabCurriculumMappingRequest.class));
    }

    @Test
    void shouldDeleteMapping() throws Exception {
        doNothing().when(mappingService).delete(1L);

        mockMvc.perform(delete("/curriculum-mappings/1"))
            .andExpect(status().isNoContent());

        verify(mappingService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentMapping() throws Exception {
        doThrow(new ResourceNotFoundException("Lab curriculum mapping not found with id: 999"))
            .when(mappingService).delete(999L);

        mockMvc.perform(delete("/curriculum-mappings/999"))
            .andExpect(status().isNotFound());

        verify(mappingService).delete(999L);
    }
}
