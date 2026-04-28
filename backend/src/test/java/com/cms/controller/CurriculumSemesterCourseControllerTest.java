package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.cms.dto.CurriculumFullViewDto;
import com.cms.dto.CurriculumSemesterCourseDto;
import com.cms.dto.CurriculumSemesterCourseRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.CurriculumSemesterCourseService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = CurriculumSemesterCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CurriculumSemesterCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CurriculumSemesterCourseService service;

    @Test
    void shouldAddCourseToSemester() throws Exception {
        CurriculumSemesterCourseRequest request = new CurriculumSemesterCourseRequest(1L, 1, 1L, 1);
        CurriculumSemesterCourseDto dto = createCscDto(1L, 1L, "BSCN-2026", 1, 1L, "Anatomy", "ANAT", 1);

        when(service.addCourseToSemester(any(CurriculumSemesterCourseRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/curriculum-semester-courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.semesterNumber").value(1))
            .andExpect(jsonPath("$.subjectName").value("Anatomy"));

        verify(service).addCourseToSemester(any(CurriculumSemesterCourseRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenCurriculumVersionIdIsNull() throws Exception {
        String json = """
            {"semesterNumber": 1, "subjectId": 1}
            """;

        mockMvc.perform(post("/curriculum-semester-courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRemoveCourseFromSemester() throws Exception {
        doNothing().when(service).removeCourseFromSemester(1L);

        mockMvc.perform(delete("/curriculum-semester-courses/1"))
            .andExpect(status().isNoContent());

        verify(service).removeCourseFromSemester(1L);
    }

    @Test
    void shouldReturnNotFoundWhenRemovingNonExistentCourse() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("not found"))
            .when(service).removeCourseFromSemester(999L);

        mockMvc.perform(delete("/curriculum-semester-courses/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetFullCurriculumWhenNoSemesterNumber() throws Exception {
        CurriculumFullViewDto fullView = new CurriculumFullViewDto(
            1L, "BSCN-2026", 1L, "BSc Nursing", 8, List.of());

        when(service.getFullCurriculum(1L)).thenReturn(fullView);

        mockMvc.perform(get("/curriculum-semester-courses").param("curriculumVersionId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.curriculumVersionId").value(1))
            .andExpect(jsonPath("$.totalSemesters").value(8));

        verify(service).getFullCurriculum(1L);
    }

    @Test
    void shouldGetCoursesBySemesterWhenSemesterNumberProvided() throws Exception {
        CurriculumSemesterCourseDto dto = createCscDto(1L, 1L, "BSCN-2026", 1, 1L, "Anatomy", "ANAT", 1);

        when(service.getCoursesBySemester(1L, 1)).thenReturn(List.of(dto));

        mockMvc.perform(get("/curriculum-semester-courses")
                .param("curriculumVersionId", "1")
                .param("semesterNumber", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].semesterNumber").value(1));

        verify(service).getCoursesBySemester(1L, 1);
    }

    private CurriculumSemesterCourseDto createCscDto(Long id, Long cvId, String cvName,
                                                      Integer semNo, Long subjId, String subjName,
                                                      String subjCode, Integer sortOrder) {
        return new CurriculumSemesterCourseDto(id, cvId, cvName, semNo, subjId, subjName, subjCode,
            sortOrder, Instant.now(), Instant.now());
    }
}
