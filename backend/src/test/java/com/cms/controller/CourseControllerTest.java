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

import com.cms.dto.CourseRequest;
import com.cms.dto.CourseResponse;
import com.cms.dto.ProgramResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    @Test
    void shouldCreateCourse() throws Exception {
        CourseRequest request = new CourseRequest(
            "B.Sc. Nursing", "BSN", "General", 1L
        );

        Instant now = Instant.now();
        ProgramResponse progResponse = new ProgramResponse(
            1L, "UG Programs", "UG", 4, 8, null, now, now);
        CourseResponse response = new CourseResponse(
            1L, "B.Sc. Nursing", "BSN", "General", progResponse, now, now
        );

        when(courseService.create(any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("B.Sc. Nursing"))
            .andExpect(jsonPath("$.code").value("BSN"))
            .andExpect(jsonPath("$.specialization").value("General"))
            .andExpect(jsonPath("$.program.id").value(1))
            .andExpect(jsonPath("$.program.durationYears").value(4));

        verify(courseService).create(any(CourseRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        CourseRequest request = new CourseRequest("", "BSN", null, 1L);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCodeIsBlank() throws Exception {
        CourseRequest request = new CourseRequest("B.Sc. Nursing", "", null, 1L);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenProgramIdIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "B.Sc. Nursing",
                "code": "BSN"
            }
            """;

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllCourses() throws Exception {
        Instant now = Instant.now();
        ProgramResponse progResponse = new ProgramResponse(
            1L, "UG Programs", "UG", 4, 8, null, now, now);
        CourseResponse course1 = new CourseResponse(
            1L, "B.Sc. Nursing", "BSN", "General", progResponse, now, now
        );
        CourseResponse course2 = new CourseResponse(
            2L, "M.Sc. Nursing", "MSN", "Obs Gyn", progResponse, now, now
        );

        when(courseService.findAll()).thenReturn(List.of(course1, course2));

        mockMvc.perform(get("/courses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("B.Sc. Nursing"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("M.Sc. Nursing"));

        verify(courseService).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoCourses() throws Exception {
        when(courseService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/courses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(courseService).findAll();
    }

    @Test
    void shouldFindCourseById() throws Exception {
        Instant now = Instant.now();
        ProgramResponse progResponse = new ProgramResponse(
            1L, "UG Programs", "UG", 4, 8, null, now, now);
        CourseResponse response = new CourseResponse(
            1L, "B.Sc. Nursing", "BSN", "General", progResponse, now, now
        );

        when(courseService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/courses/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("B.Sc. Nursing"))
            .andExpect(jsonPath("$.code").value("BSN"));

        verify(courseService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenCourseNotExists() throws Exception {
        when(courseService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Course not found with id: 999"));

        mockMvc.perform(get("/courses/999"))
            .andExpect(status().isNotFound());

        verify(courseService).findById(999L);
    }

    @Test
    void shouldFindCoursesByProgramId() throws Exception {
        Instant now = Instant.now();
        ProgramResponse progResponse = new ProgramResponse(
            1L, "UG Programs", "UG", 4, 8, null, now, now);
        CourseResponse course1 = new CourseResponse(
            1L, "B.Sc. Nursing", "BSN", "General", progResponse, now, now
        );
        CourseResponse course2 = new CourseResponse(
            2L, "M.Sc. Nursing", "MSN", "Obs Gyn", progResponse, now, now
        );

        when(courseService.findByProgramId(1L)).thenReturn(List.of(course1, course2));

        mockMvc.perform(get("/courses/program/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2));

        verify(courseService).findByProgramId(1L);
    }

    @Test
    void shouldReturnNotFoundWhenFindingCoursesByNonExistentProgram() throws Exception {
        when(courseService.findByProgramId(999L))
            .thenThrow(new ResourceNotFoundException("Program not found with id: 999"));

        mockMvc.perform(get("/courses/program/999"))
            .andExpect(status().isNotFound());

        verify(courseService).findByProgramId(999L);
    }

    @Test
    void shouldUpdateCourse() throws Exception {
        CourseRequest request = new CourseRequest(
            "B.Sc. Nursing Updated", "BSNU", "Updated Specialization", 1L
        );

        Instant now = Instant.now();
        ProgramResponse progResponse = new ProgramResponse(
            1L, "UG Programs", "UG", 4, 8, null, now, now);
        CourseResponse response = new CourseResponse(
            1L, "B.Sc. Nursing Updated", "BSNU", "Updated Specialization", progResponse, now, now
        );

        when(courseService.update(eq(1L), any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(put("/courses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("B.Sc. Nursing Updated"))
            .andExpect(jsonPath("$.code").value("BSNU"));

        verify(courseService).update(eq(1L), any(CourseRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentCourse() throws Exception {
        CourseRequest request = new CourseRequest("Name", "CODE", null, 1L);

        when(courseService.update(eq(999L), any(CourseRequest.class)))
            .thenThrow(new ResourceNotFoundException("Course not found with id: 999"));

        mockMvc.perform(put("/courses/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(courseService).update(eq(999L), any(CourseRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidData() throws Exception {
        CourseRequest request = new CourseRequest("", "", null, 1L);

        mockMvc.perform(put("/courses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteCourse() throws Exception {
        doNothing().when(courseService).delete(1L);

        mockMvc.perform(delete("/courses/1"))
            .andExpect(status().isNoContent());

        verify(courseService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentCourse() throws Exception {
        doThrow(new ResourceNotFoundException("Course not found with id: 999"))
            .when(courseService).delete(999L);

        mockMvc.perform(delete("/courses/999"))
            .andExpect(status().isNotFound());

        verify(courseService).delete(999L);
    }
}
