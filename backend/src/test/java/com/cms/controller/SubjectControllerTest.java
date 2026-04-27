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

import com.cms.dto.CourseResponse;
import com.cms.dto.DepartmentResponse;
import com.cms.dto.ProgramResponse;
import com.cms.dto.SubjectRequest;
import com.cms.dto.SubjectResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.SubjectService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SubjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubjectService subjectService;

    private final Instant now = Instant.now();

    private SubjectResponse createTestResponse(Long id, String name, String code) {
        DepartmentResponse dept = new DepartmentResponse(1L, "MSN", "MSN", "Desc", "Dr. X", now, now);
        ProgramResponse prog = new ProgramResponse(1L, "B.Sc. Nursing", "BSCN",
            4, 8, null, now, now);
        CourseResponse courseResp = new CourseResponse(1L, "BSN Course", "BSN",
            "General", prog, now, now);
        return new SubjectResponse(id, name, code, 4, 3, 1, courseResp, dept, 1, now, now);
    }

    @Test
    void shouldCreateSubject() throws Exception {
        SubjectRequest request = new SubjectRequest("Anatomy", "ANAT101", 4, 3, 1, 1L, 1L, 1);
        SubjectResponse response = createTestResponse(1L, "Anatomy", "ANAT101");

        when(subjectService.create(any(SubjectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/subjects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Anatomy"))
            .andExpect(jsonPath("$.code").value("ANAT101"))
            .andExpect(jsonPath("$.credits").value(4));

        verify(subjectService).create(any(SubjectRequest.class));
    }

    @Test
    void shouldFindAllSubjects() throws Exception {
        SubjectResponse response = createTestResponse(1L, "Anatomy", "ANAT101");
        when(subjectService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/subjects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Anatomy"));

        verify(subjectService).findAll();
    }

    @Test
    void shouldFindSubjectById() throws Exception {
        SubjectResponse response = createTestResponse(1L, "Anatomy", "ANAT101");
        when(subjectService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/subjects/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Anatomy"));

        verify(subjectService).findById(1L);
    }

    @Test
    void shouldReturn404WhenSubjectNotFound() throws Exception {
        when(subjectService.findById(999L)).thenThrow(
            new ResourceNotFoundException("Subject not found with id: 999"));

        mockMvc.perform(get("/subjects/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldFindSubjectsByCourseId() throws Exception {
        SubjectResponse response = createTestResponse(1L, "Anatomy", "ANAT101");
        when(subjectService.findByCourseId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/subjects/course/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Anatomy"));

        verify(subjectService).findByCourseId(1L);
    }

    @Test
    void shouldFindSubjectsByDepartmentId() throws Exception {
        SubjectResponse response = createTestResponse(1L, "Anatomy", "ANAT101");
        when(subjectService.findByDepartmentId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/subjects/department/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Anatomy"));

        verify(subjectService).findByDepartmentId(1L);
    }

    @Test
    void shouldUpdateSubject() throws Exception {
        SubjectRequest request = new SubjectRequest("Physiology", "PHYS101", 5, 4, 1, 1L, 1L, 2);
        SubjectResponse response = createTestResponse(1L, "Physiology", "PHYS101");

        when(subjectService.update(eq(1L), any(SubjectRequest.class))).thenReturn(response);

        mockMvc.perform(put("/subjects/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Physiology"));

        verify(subjectService).update(eq(1L), any(SubjectRequest.class));
    }

    @Test
    void shouldDeleteSubject() throws Exception {
        doNothing().when(subjectService).delete(1L);

        mockMvc.perform(delete("/subjects/1"))
            .andExpect(status().isNoContent());

        verify(subjectService).delete(1L);
    }

    @Test
    void shouldReturn404WhenDeleteNonExistentSubject() throws Exception {
        doThrow(new ResourceNotFoundException("Subject not found with id: 999"))
            .when(subjectService).delete(999L);

        mockMvc.perform(delete("/subjects/999"))
            .andExpect(status().isNotFound());
    }
}

