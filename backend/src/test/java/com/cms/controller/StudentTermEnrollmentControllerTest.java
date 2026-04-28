package com.cms.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.StudentTermEnrollmentDto;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.service.StudentTermEnrollmentService;

@WebMvcTest(controllers = StudentTermEnrollmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentTermEnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentTermEnrollmentService service;

    private StudentTermEnrollmentDto createDto(Long id, Long studentId, Long cohortId, Long termInstanceId,
                                                Integer semesterNumber, Integer yearOfStudy) {
        return new StudentTermEnrollmentDto(
            id, studentId, "John Doe", cohortId, "BCA-2024-2027",
            termInstanceId, "2024-2025 ODD", semesterNumber, yearOfStudy, EnrollmentStatus.ENROLLED
        );
    }

    @Test
    void getEnrollmentsByTermInstance() throws Exception {
        StudentTermEnrollmentDto dto = createDto(1L, 1L, 1L, 1L, 1, 1);
        when(service.getEnrollmentsByTermInstance(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/student-term-enrollments").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].semesterNumber").value(1));

        verify(service).getEnrollmentsByTermInstance(1L);
    }

    @Test
    void getEnrollmentsByTermInstanceAndSemester() throws Exception {
        StudentTermEnrollmentDto dto = createDto(1L, 1L, 1L, 1L, 1, 1);
        when(service.getEnrollmentsByTermInstanceAndSemester(1L, 1)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/student-term-enrollments")
                .param("termInstanceId", "1")
                .param("semesterNumber", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(service).getEnrollmentsByTermInstanceAndSemester(1L, 1);
    }

    @Test
    void getEnrollmentsByStudent() throws Exception {
        StudentTermEnrollmentDto dto = createDto(1L, 1L, 1L, 1L, 1, 1);
        when(service.getEnrollmentsByStudent(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/student-term-enrollments").param("studentId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(service).getEnrollmentsByStudent(1L);
    }

    @Test
    void getEnrollments_badRequestWhenNoParams() throws Exception {
        mockMvc.perform(get("/api/student-term-enrollments"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getById() throws Exception {
        StudentTermEnrollmentDto dto = createDto(1L, 1L, 1L, 1L, 1, 1);
        when(service.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/student-term-enrollments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.studentName").value("John Doe"));

        verify(service).getById(1L);
    }

    @Test
    void generate() throws Exception {
        when(service.generateEnrollmentsForTermInstance(1L)).thenReturn(5);

        mockMvc.perform(post("/api/student-term-enrollments/generate").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enrollmentsCreated").value(5));

        verify(service).generateEnrollmentsForTermInstance(1L);
    }
}
