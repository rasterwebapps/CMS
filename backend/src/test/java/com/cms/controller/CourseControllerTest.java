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
import com.cms.dto.DepartmentResponse;
import com.cms.dto.ProgramResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.DegreeType;
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
            "Data Structures",
            "CS201",
            4,
            3,
            1,
            1L,
            3
        );

        Instant now = Instant.now();
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        ProgramResponse progResponse = new ProgramResponse(
            1L, "Bachelor of CS", "BCS", DegreeType.BACHELOR, 4, deptResponse, now, now
        );
        CourseResponse response = new CourseResponse(
            1L,
            "Data Structures",
            "CS201",
            4,
            3,
            1,
            progResponse,
            3,
            now,
            now
        );

        when(courseService.create(any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Data Structures"))
            .andExpect(jsonPath("$.code").value("CS201"))
            .andExpect(jsonPath("$.credits").value(4))
            .andExpect(jsonPath("$.theoryCredits").value(3))
            .andExpect(jsonPath("$.labCredits").value(1))
            .andExpect(jsonPath("$.semester").value(3))
            .andExpect(jsonPath("$.program.id").value(1));

        verify(courseService).create(any(CourseRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        CourseRequest request = new CourseRequest(
            "",
            "CS201",
            4,
            3,
            1,
            1L,
            3
        );

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCodeIsBlank() throws Exception {
        CourseRequest request = new CourseRequest(
            "Data Structures",
            "",
            4,
            3,
            1,
            1L,
            3
        );

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCreditsIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Data Structures",
                "code": "CS201",
                "theoryCredits": 3,
                "labCredits": 1,
                "programId": 1,
                "semester": 3
            }
            """;

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenTheoryCreditsIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Data Structures",
                "code": "CS201",
                "credits": 4,
                "labCredits": 1,
                "programId": 1,
                "semester": 3
            }
            """;

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenLabCreditsIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Data Structures",
                "code": "CS201",
                "credits": 4,
                "theoryCredits": 3,
                "programId": 1,
                "semester": 3
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
                "name": "Data Structures",
                "code": "CS201",
                "credits": 4,
                "theoryCredits": 3,
                "labCredits": 1,
                "semester": 3
            }
            """;

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSemesterIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Data Structures",
                "code": "CS201",
                "credits": 4,
                "theoryCredits": 3,
                "labCredits": 1,
                "programId": 1
            }
            """;

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCreditsTooLow() throws Exception {
        CourseRequest request = new CourseRequest(
            "Data Structures",
            "CS201",
            0,
            3,
            1,
            1L,
            3
        );

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCreditsTooHigh() throws Exception {
        CourseRequest request = new CourseRequest(
            "Data Structures",
            "CS201",
            21,
            3,
            1,
            1L,
            3
        );

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenTheoryCreditsTooLow() throws Exception {
        CourseRequest request = new CourseRequest(
            "Data Structures",
            "CS201",
            4,
            -1,
            1,
            1L,
            3
        );

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenLabCreditsTooLow() throws Exception {
        CourseRequest request = new CourseRequest(
            "Data Structures",
            "CS201",
            4,
            3,
            -1,
            1L,
            3
        );

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSemesterTooLow() throws Exception {
        CourseRequest request = new CourseRequest(
            "Data Structures",
            "CS201",
            4,
            3,
            1,
            1L,
            0
        );

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSemesterTooHigh() throws Exception {
        CourseRequest request = new CourseRequest(
            "Data Structures",
            "CS201",
            4,
            3,
            1,
            1L,
            13
        );

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllCourses() throws Exception {
        Instant now = Instant.now();
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        ProgramResponse progResponse = new ProgramResponse(
            1L, "Bachelor of CS", "BCS", DegreeType.BACHELOR, 4, deptResponse, now, now
        );
        CourseResponse course1 = new CourseResponse(
            1L, "Data Structures", "CS201", 4, 3, 1, progResponse, 3, now, now
        );
        CourseResponse course2 = new CourseResponse(
            2L, "Algorithms", "CS301", 4, 4, 0, progResponse, 5, now, now
        );

        when(courseService.findAll()).thenReturn(List.of(course1, course2));

        mockMvc.perform(get("/courses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Data Structures"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("Algorithms"));

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
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        ProgramResponse progResponse = new ProgramResponse(
            1L, "Bachelor of CS", "BCS", DegreeType.BACHELOR, 4, deptResponse, now, now
        );
        CourseResponse response = new CourseResponse(
            1L,
            "Data Structures",
            "CS201",
            4,
            3,
            1,
            progResponse,
            3,
            now,
            now
        );

        when(courseService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/courses/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Data Structures"))
            .andExpect(jsonPath("$.code").value("CS201"));

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
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        ProgramResponse progResponse = new ProgramResponse(
            1L, "Bachelor of CS", "BCS", DegreeType.BACHELOR, 4, deptResponse, now, now
        );
        CourseResponse course1 = new CourseResponse(
            1L, "Data Structures", "CS201", 4, 3, 1, progResponse, 3, now, now
        );
        CourseResponse course2 = new CourseResponse(
            2L, "Algorithms", "CS301", 4, 4, 0, progResponse, 5, now, now
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
            "Advanced Data Structures",
            "CS401",
            5,
            3,
            2,
            1L,
            4
        );

        Instant now = Instant.now();
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        ProgramResponse progResponse = new ProgramResponse(
            1L, "Bachelor of CS", "BCS", DegreeType.BACHELOR, 4, deptResponse, now, now
        );
        CourseResponse response = new CourseResponse(
            1L,
            "Advanced Data Structures",
            "CS401",
            5,
            3,
            2,
            progResponse,
            4,
            now,
            now
        );

        when(courseService.update(eq(1L), any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(put("/courses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Advanced Data Structures"))
            .andExpect(jsonPath("$.code").value("CS401"));

        verify(courseService).update(eq(1L), any(CourseRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentCourse() throws Exception {
        CourseRequest request = new CourseRequest(
            "Name",
            "CODE",
            4,
            3,
            1,
            1L,
            1
        );

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
        CourseRequest request = new CourseRequest(
            "",
            "",
            4,
            3,
            1,
            1L,
            1
        );

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
