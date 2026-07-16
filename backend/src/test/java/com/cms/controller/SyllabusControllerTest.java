package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import com.cms.dto.SyllabusActivationRequest;
import com.cms.dto.SyllabusRequest;
import com.cms.dto.SyllabusResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.SyllabusService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SyllabusController.class)
@AutoConfigureMockMvc(addFilters = false)
class SyllabusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SyllabusService syllabusService;

    @Test
    void shouldCreateSyllabus() throws Exception {
        SyllabusRequest request = new SyllabusRequest(
            1L,
            "Objectives", "Content", "Text books",
            "Ref books", "Outcomes", true
        );

        SyllabusResponse response = sampleResponse(1L, 1, 30, 15, 10, true);

        when(syllabusService.create(any(SyllabusRequest.class))).thenReturn(response);

        mockMvc.perform(post("/syllabi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.subjectId").value(1))
            .andExpect(jsonPath("$.version").value(1));

        verify(syllabusService).create(any(SyllabusRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenCurriculumTermCourseIdIsNull() throws Exception {
        SyllabusRequest request = new SyllabusRequest(
            null,
            "Objectives", "Content", "Text books",
            "Ref books", "Outcomes", true
        );

        mockMvc.perform(post("/syllabi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllSyllabi() throws Exception {
        SyllabusResponse response = sampleResponse(1L, 1, 30, 15, 10, true);

        when(syllabusService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/syllabi"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(syllabusService).findAll();
    }

    @Test
    void shouldFindSyllabiBySubjectId() throws Exception {
        SyllabusResponse response = sampleResponse(1L, 1, 30, 15, 10, true);

        when(syllabusService.findBySubjectId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/syllabi").param("subjectId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].subjectId").value(1));

        verify(syllabusService).findBySubjectId(1L);
    }

    @Test
    void shouldFindSyllabusById() throws Exception {
        SyllabusResponse response = sampleResponse(1L, 1, 30, 15, 10, true);

        when(syllabusService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/syllabi/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.subjectName").value("Data Structures"));

        verify(syllabusService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenSyllabusNotExists() throws Exception {
        when(syllabusService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Syllabus not found with id: 999"));

        mockMvc.perform(get("/syllabi/999"))
            .andExpect(status().isNotFound());

        verify(syllabusService).findById(999L);
    }

    @Test
    void shouldActivateSyllabus() throws Exception {
        SyllabusActivationRequest request = new SyllabusActivationRequest(true);
        SyllabusResponse response = sampleResponse(1L, 2, 40, 20, 15, true);

        when(syllabusService.setActive(eq(1L), any(SyllabusActivationRequest.class))).thenReturn(response);

        mockMvc.perform(put("/syllabi/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(true));

        verify(syllabusService).setActive(eq(1L), any(SyllabusActivationRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenActivatingNonExistentSyllabus() throws Exception {
        when(syllabusService.setActive(eq(999L), any(SyllabusActivationRequest.class)))
            .thenThrow(new ResourceNotFoundException("Syllabus not found with id: 999"));

        mockMvc.perform(put("/syllabi/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SyllabusActivationRequest(false))))
            .andExpect(status().isNotFound());
    }

    private SyllabusResponse sampleResponse(Long id, Integer version, Integer theoryHours, Integer labHours,
                                             Integer clinicalHours, Boolean isActive) {
        Instant now = Instant.now();
        return new SyllabusResponse(
            id, 1L, 1L, "BSCN-2026", 1,
            1L, "Data Structures", "CS201",
            version, theoryHours, labHours, clinicalHours,
            "Objectives", "Content", "Text books", "Ref books", "Outcomes",
            isActive, now, now
        );
    }
}
