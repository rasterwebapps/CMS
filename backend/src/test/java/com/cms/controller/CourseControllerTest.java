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
import com.cms.model.enums.DegreeType;
import com.cms.model.enums.ProgramLevel;
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
            "B.Sc. Nursing", "BSN", DegreeType.BACHELOR, 4, 1L
        );

        Instant now = Instant.now();
        ProgramResponse progResponse = new ProgramResponse(
            1L, "UG Programs", "UG", ProgramLevel.UNDERGRADUATE, List.of(), now, now
        );
        CourseResponse response = new CourseResponse(
            1L, "B.Sc. Nursing", "BSN", DegreeType.BACHELOR, 4, progResponse, now, now
        );

        when(courseService.create(any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("B.Sc. Nursing"))
            .andExpect(jsonPath("$.code").value("BSN"))
            .andExpect(jsonPath("$.degreeType").value("BACHELOR"))
            .andExpect(jsonPath("$.durationYears").value(4))
            .andExpect(jsonPath("$.program.id").value(1));

        verify(courseService).create(any(CourseRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        CourseRequest request = new CourseRequest("", "BSN", DegreeType.BACHELOR, 4, 1L);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCodeIsBlank() throws Exception {
        CourseRequest request = new CourseRequest("B.Sc. Nursing", "", DegreeType.BACHELOR, 4, 1L);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenDegreeTypeIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "B.Sc. Nursing",
                "code": "BSN",
                "durationYears": 4,
                "programId": 1
            }
            """;

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenDurationYearsIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "B.Sc. Nursing",
                "code": "BSN",
                "degreeType": "BACHELOR",
                "programId": 1
            }
            """;

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenProgramIdIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "B.Sc. Nursing",
                "code": "BSN",
                "degreeType": "BACHELOR",
                "durationYears": 4
            }
            """;

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenDurationYearsTooLow() throws Exception {
        CourseRequest request = new CourseRequest("B.Sc. Nursing", "BSN", DegreeType.BACHELOR, 0, 1L);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenDurationYearsTooHigh() throws Exception {
        CourseRequest request = new CourseRequest("B.Sc. Nursing", "BSN", DegreeType.BACHELOR, 11, 1L);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllCourses() throws Exception {
        Instant now = Instant.now();
        ProgramResponse progResponse = new ProgramResponse(
            1L, "UG Programs", "UG", ProgramLevel.UNDERGRADUATE, List.of(), now, now
        );
        CourseResponse course1 = new CourseResponse(
            1L, "B.Sc. Nursing", "BSN", DegreeType.BACHELOR, 4, progResponse, now, now
        );
        CourseResponse course2 = new CourseResponse(
            2L, "M.Sc. Nursing", "MSN", DegreeType.MASTER, 2, progResponse, now, now
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
            1L, "UG Programs", "UG", ProgramLevel.UNDERGRADUATE, List.of(), now, now
        );
        CourseResponse response = new CourseResponse(
            1L, "B.Sc. Nursing", "BSN", DegreeType.BACHELOR, 4, progResponse, now, now
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
            1L, "UG Programs", "UG", ProgramLevel.UNDERGRADUATE, List.of(), now, now
        );
        CourseResponse course1 = new CourseResponse(
            1L, "B.Sc. Nursing", "BSN", DegreeType.BACHELOR, 4, progResponse, now, now
        );
        CourseResponse course2 = new CourseResponse(
            2L, "M.Sc. Nursing", "MSN", DegreeType.MASTER, 2, progResponse, now, now
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
            "B.Sc. Nursing Updated", "BSNU", DegreeType.BACHELOR, 5, 1L
        );

        Instant now = Instant.now();
        ProgramResponse progResponse = new ProgramResponse(
            1L, "UG Programs", "UG", ProgramLevel.UNDERGRADUATE, List.of(), now, now
        );
        CourseResponse response = new CourseResponse(
            1L, "B.Sc. Nursing Updated", "BSNU", DegreeType.BACHELOR, 5, progResponse, now, now
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
        CourseRequest request = new CourseRequest("Name", "CODE", DegreeType.BACHELOR, 4, 1L);

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
        CourseRequest request = new CourseRequest("", "", DegreeType.BACHELOR, 4, 1L);

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
