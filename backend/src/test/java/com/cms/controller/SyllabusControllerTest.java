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
            1L, 1, 30, 15, 10,
            "Objectives", "Content", "Text books",
            "Ref books", "Outcomes", true
        );

        Instant now = Instant.now();
        SyllabusResponse response = new SyllabusResponse(
            1L, 1L, "Data Structures", "CS201",
            1, 30, 15, 10, "Objectives", "Content",
            "Text books", "Ref books", "Outcomes",
            true, now, now
        );

        when(syllabusService.create(any(SyllabusRequest.class))).thenReturn(response);

        mockMvc.perform(post("/syllabi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.courseId").value(1))
            .andExpect(jsonPath("$.version").value(1));

        verify(syllabusService).create(any(SyllabusRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenCourseIdIsNull() throws Exception {
        SyllabusRequest request = new SyllabusRequest(
            null, 1, 30, 15, 10,
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
        Instant now = Instant.now();
        SyllabusResponse response = new SyllabusResponse(
            1L, 1L, "Data Structures", "CS201",
            1, 30, 15, 10, "Objectives", "Content",
            "Text books", "Ref books", "Outcomes",
            true, now, now
        );

        when(syllabusService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/syllabi"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(syllabusService).findAll();
    }

    @Test
    void shouldFindSyllabiByCourseId() throws Exception {
        Instant now = Instant.now();
        SyllabusResponse response = new SyllabusResponse(
            1L, 1L, "Data Structures", "CS201",
            1, 30, 15, 10, "Objectives", "Content",
            "Text books", "Ref books", "Outcomes",
            true, now, now
        );

        when(syllabusService.findByCourseId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/syllabi").param("courseId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].courseId").value(1));

        verify(syllabusService).findByCourseId(1L);
    }

    @Test
    void shouldFindSyllabusById() throws Exception {
        Instant now = Instant.now();
        SyllabusResponse response = new SyllabusResponse(
            1L, 1L, "Data Structures", "CS201",
            1, 30, 15, 10, "Objectives", "Content",
            "Text books", "Ref books", "Outcomes",
            true, now, now
        );

        when(syllabusService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/syllabi/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.courseName").value("Data Structures"));

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
    void shouldUpdateSyllabus() throws Exception {
        SyllabusRequest request = new SyllabusRequest(
            1L, 2, 40, 20, 15,
            "Updated", "Updated", "Updated",
            "Updated", "Updated", true
        );

        Instant now = Instant.now();
        SyllabusResponse response = new SyllabusResponse(
            1L, 1L, "Data Structures", "CS201",
            2, 40, 20, 15, "Updated", "Updated",
            "Updated", "Updated", "Updated",
            true, now, now
        );

        when(syllabusService.update(eq(1L), any(SyllabusRequest.class))).thenReturn(response);

        mockMvc.perform(put("/syllabi/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2));

        verify(syllabusService).update(eq(1L), any(SyllabusRequest.class));
    }

    @Test
    void shouldDeleteSyllabus() throws Exception {
        doNothing().when(syllabusService).delete(1L);

        mockMvc.perform(delete("/syllabi/1"))
            .andExpect(status().isNoContent());

        verify(syllabusService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentSyllabus() throws Exception {
        doThrow(new ResourceNotFoundException("Syllabus not found with id: 999"))
            .when(syllabusService).delete(999L);

        mockMvc.perform(delete("/syllabi/999"))
            .andExpect(status().isNotFound());

        verify(syllabusService).delete(999L);
    }
}
