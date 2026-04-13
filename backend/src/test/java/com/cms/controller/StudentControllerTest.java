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
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.StudentRequest;
import com.cms.dto.StudentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.StudentStatus;
import com.cms.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentService studentService;

    @Test
    void shouldCreateStudent() throws Exception {
        StudentRequest request = new StudentRequest(
            "CS2024001", "John", "Doe", "john@college.edu", "1234567890",
            1L, 1, LocalDate.of(2024, 6, 1), "Batch-A", StudentStatus.ACTIVE,
            null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        StudentResponse response = createStudentResponse(1L, "CS2024001", "John", "Doe");

        when(studentService.create(any(StudentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.rollNumber").value("CS2024001"))
            .andExpect(jsonPath("$.fullName").value("John Doe"));

        verify(studentService).create(any(StudentRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenRollNumberIsBlank() throws Exception {
        StudentRequest request = new StudentRequest(
            "", "John", "Doe", "john@college.edu", "1234567890",
            1L, 1, LocalDate.of(2024, 6, 1), null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        mockMvc.perform(post("/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllStudents() throws Exception {
        StudentResponse response = createStudentResponse(1L, "CS2024001", "John", "Doe");

        when(studentService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/students"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1));

        verify(studentService).findAll();
    }

    @Test
    void shouldFindStudentsByProgramId() throws Exception {
        StudentResponse response = createStudentResponse(1L, "CS2024001", "John", "Doe");

        when(studentService.findByProgramId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/students").param("programId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].programId").value(1));

        verify(studentService).findByProgramId(1L);
    }

    @Test
    void shouldFindStudentById() throws Exception {
        StudentResponse response = createStudentResponse(1L, "CS2024001", "John", "Doe");

        when(studentService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/students/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.rollNumber").value("CS2024001"));

        verify(studentService).findById(1L);
    }

    @Test
    void shouldFindStudentByRollNumber() throws Exception {
        StudentResponse response = createStudentResponse(1L, "CS2024001", "John", "Doe");

        when(studentService.findByRollNumber("CS2024001")).thenReturn(response);

        mockMvc.perform(get("/students/roll-number/CS2024001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rollNumber").value("CS2024001"));

        verify(studentService).findByRollNumber("CS2024001");
    }

    @Test
    void shouldReturnNotFoundWhenStudentNotExists() throws Exception {
        when(studentService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

        mockMvc.perform(get("/students/999"))
            .andExpect(status().isNotFound());

        verify(studentService).findById(999L);
    }

    @Test
    void shouldUpdateStudent() throws Exception {
        StudentRequest request = new StudentRequest(
            "CS2024001", "Johnny", "Doe", "johnny@college.edu", "9999999999",
            1L, 2, LocalDate.of(2024, 6, 1), "Batch-B", StudentStatus.ACTIVE,
            null, null, null, null, null, null, null, null,
            null, null, null, null
        );

        StudentResponse response = createStudentResponse(1L, "CS2024001", "Johnny", "Doe");

        when(studentService.update(eq(1L), any(StudentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/students/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Johnny"));

        verify(studentService).update(eq(1L), any(StudentRequest.class));
    }

    @Test
    void shouldDeleteStudent() throws Exception {
        doNothing().when(studentService).delete(1L);

        mockMvc.perform(delete("/students/1"))
            .andExpect(status().isNoContent());

        verify(studentService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentStudent() throws Exception {
        doThrow(new ResourceNotFoundException("Student not found with id: 999"))
            .when(studentService).delete(999L);

        mockMvc.perform(delete("/students/999"))
            .andExpect(status().isNotFound());

        verify(studentService).delete(999L);
    }

    private StudentResponse createStudentResponse(Long id, String rollNumber, String firstName, String lastName) {
        Instant now = Instant.now();
        return new StudentResponse(
            id, rollNumber, firstName, lastName, firstName + " " + lastName,
            firstName.toLowerCase() + "@college.edu", "1234567890", 1L, "B.Tech Computer Science",
            1, LocalDate.of(2024, 6, 1), "Batch-A", StudentStatus.ACTIVE,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            now, now
        );
    }
}
