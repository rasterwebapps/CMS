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

import com.cms.dto.DepartmentRequest;
import com.cms.dto.DepartmentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DepartmentService departmentService;

    @Test
    void shouldCreateDepartment() throws Exception {
        DepartmentRequest request = new DepartmentRequest(
            "Computer Science",
            "CS",
            "Department of Computer Science",
            "Dr. John Doe"
        );

        Instant now = Instant.now();
        DepartmentResponse response = new DepartmentResponse(
            1L,
            "Computer Science",
            "CS",
            "Department of Computer Science",
            "Dr. John Doe",
            now,
            now
        );

        when(departmentService.create(any(DepartmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Computer Science"))
            .andExpect(jsonPath("$.code").value("CS"))
            .andExpect(jsonPath("$.description").value("Department of Computer Science"))
            .andExpect(jsonPath("$.hodName").value("Dr. John Doe"));

        verify(departmentService).create(any(DepartmentRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        DepartmentRequest request = new DepartmentRequest(
            "",
            "CS",
            "Description",
            "Dr. John Doe"
        );

        mockMvc.perform(post("/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCodeIsBlank() throws Exception {
        DepartmentRequest request = new DepartmentRequest(
            "Computer Science",
            "",
            "Description",
            "Dr. John Doe"
        );

        mockMvc.perform(post("/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenNameIsNull() throws Exception {
        String jsonRequest = """
            {
                "code": "CS",
                "description": "Description",
                "hodName": "Dr. John Doe"
            }
            """;

        mockMvc.perform(post("/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllDepartments() throws Exception {
        Instant now = Instant.now();
        DepartmentResponse dept1 = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Dept", "Dr. A", now, now
        );
        DepartmentResponse dept2 = new DepartmentResponse(
            2L, "Mathematics", "MATH", "Math Dept", "Dr. B", now, now
        );

        when(departmentService.findAll()).thenReturn(List.of(dept1, dept2));

        mockMvc.perform(get("/departments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Computer Science"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("Mathematics"));

        verify(departmentService).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoDepartments() throws Exception {
        when(departmentService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/departments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(departmentService).findAll();
    }

    @Test
    void shouldFindDepartmentById() throws Exception {
        Instant now = Instant.now();
        DepartmentResponse response = new DepartmentResponse(
            1L,
            "Computer Science",
            "CS",
            "Department of Computer Science",
            "Dr. John Doe",
            now,
            now
        );

        when(departmentService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/departments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Computer Science"))
            .andExpect(jsonPath("$.code").value("CS"));

        verify(departmentService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDepartmentNotExists() throws Exception {
        when(departmentService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Department not found with id: 999"));

        mockMvc.perform(get("/departments/999"))
            .andExpect(status().isNotFound());

        verify(departmentService).findById(999L);
    }

    @Test
    void shouldUpdateDepartment() throws Exception {
        DepartmentRequest request = new DepartmentRequest(
            "Computer Science Updated",
            "CSU",
            "Updated Description",
            "Dr. New HOD"
        );

        Instant now = Instant.now();
        DepartmentResponse response = new DepartmentResponse(
            1L,
            "Computer Science Updated",
            "CSU",
            "Updated Description",
            "Dr. New HOD",
            now,
            now
        );

        when(departmentService.update(eq(1L), any(DepartmentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/departments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Computer Science Updated"))
            .andExpect(jsonPath("$.code").value("CSU"));

        verify(departmentService).update(eq(1L), any(DepartmentRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentDepartment() throws Exception {
        DepartmentRequest request = new DepartmentRequest(
            "Name",
            "CODE",
            "Description",
            "HOD"
        );

        when(departmentService.update(eq(999L), any(DepartmentRequest.class)))
            .thenThrow(new ResourceNotFoundException("Department not found with id: 999"));

        mockMvc.perform(put("/departments/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(departmentService).update(eq(999L), any(DepartmentRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidData() throws Exception {
        DepartmentRequest request = new DepartmentRequest(
            "",
            "",
            "Description",
            "HOD"
        );

        mockMvc.perform(put("/departments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteDepartment() throws Exception {
        doNothing().when(departmentService).delete(1L);

        mockMvc.perform(delete("/departments/1"))
            .andExpect(status().isNoContent());

        verify(departmentService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentDepartment() throws Exception {
        doThrow(new ResourceNotFoundException("Department not found with id: 999"))
            .when(departmentService).delete(999L);

        mockMvc.perform(delete("/departments/999"))
            .andExpect(status().isNotFound());

        verify(departmentService).delete(999L);
    }
}
