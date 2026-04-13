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

import com.cms.dto.FacultyRequest;
import com.cms.dto.FacultyResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.Designation;
import com.cms.model.enums.FacultyStatus;
import com.cms.service.FacultyService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = FacultyController.class)
@AutoConfigureMockMvc(addFilters = false)
class FacultyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FacultyService facultyService;

    @Test
    void shouldCreateFaculty() throws Exception {
        FacultyRequest request = new FacultyRequest(
            "EMP001",
            "John",
            "Doe",
            "john.doe@college.edu",
            "1234567890",
            1L,
            Designation.PROFESSOR,
            "Artificial Intelligence",
            "Machine Learning Lab",
            LocalDate.of(2020, 1, 15),
            FacultyStatus.ACTIVE
        );

        Instant now = Instant.now();
        FacultyResponse response = new FacultyResponse(
            1L,
            "EMP001",
            "John",
            "Doe",
            "John Doe",
            "john.doe@college.edu",
            "1234567890",
            1L,
            "Computer Science",
            Designation.PROFESSOR,
            "Artificial Intelligence",
            "Machine Learning Lab",
            LocalDate.of(2020, 1, 15),
            FacultyStatus.ACTIVE,
            now,
            now
        );

        when(facultyService.create(any(FacultyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/faculty")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.employeeCode").value("EMP001"))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.fullName").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john.doe@college.edu"))
            .andExpect(jsonPath("$.departmentId").value(1))
            .andExpect(jsonPath("$.departmentName").value("Computer Science"))
            .andExpect(jsonPath("$.designation").value("PROFESSOR"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(facultyService).create(any(FacultyRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenEmployeeCodeIsBlank() throws Exception {
        FacultyRequest request = new FacultyRequest(
            "",
            "John",
            "Doe",
            "john.doe@college.edu",
            "1234567890",
            1L,
            Designation.PROFESSOR,
            "AI",
            "ML Lab",
            LocalDate.of(2020, 1, 15),
            FacultyStatus.ACTIVE
        );

        mockMvc.perform(post("/faculty")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        FacultyRequest request = new FacultyRequest(
            "EMP001",
            "John",
            "Doe",
            "invalid-email",
            "1234567890",
            1L,
            Designation.PROFESSOR,
            "AI",
            "ML Lab",
            LocalDate.of(2020, 1, 15),
            FacultyStatus.ACTIVE
        );

        mockMvc.perform(post("/faculty")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenDepartmentIdIsNull() throws Exception {
        String jsonRequest = """
            {
                "employeeCode": "EMP001",
                "firstName": "John",
                "lastName": "Doe",
                "email": "john.doe@college.edu",
                "phone": "1234567890",
                "designation": "PROFESSOR",
                "specialization": "AI",
                "labExpertise": "ML Lab",
                "joiningDate": "2020-01-15",
                "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/faculty")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllFaculty() throws Exception {
        Instant now = Instant.now();
        FacultyResponse faculty1 = new FacultyResponse(
            1L, "EMP001", "John", "Doe", "John Doe", "john@college.edu", "123",
            1L, "Computer Science", Designation.PROFESSOR, "AI", "ML Lab",
            LocalDate.of(2020, 1, 15), FacultyStatus.ACTIVE, now, now
        );
        FacultyResponse faculty2 = new FacultyResponse(
            2L, "EMP002", "Jane", "Smith", "Jane Smith", "jane@college.edu", "456",
            1L, "Computer Science", Designation.ASSISTANT_PROFESSOR, "Data Science", "Data Lab",
            LocalDate.of(2021, 6, 1), FacultyStatus.ACTIVE, now, now
        );

        when(facultyService.findAll()).thenReturn(List.of(faculty1, faculty2));

        mockMvc.perform(get("/faculty"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].employeeCode").value("EMP001"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].employeeCode").value("EMP002"));

        verify(facultyService).findAll();
    }

    @Test
    void shouldFindFacultyByDepartmentId() throws Exception {
        Instant now = Instant.now();
        FacultyResponse faculty = new FacultyResponse(
            1L, "EMP001", "John", "Doe", "John Doe", "john@college.edu", "123",
            1L, "Computer Science", Designation.PROFESSOR, "AI", "ML Lab",
            LocalDate.of(2020, 1, 15), FacultyStatus.ACTIVE, now, now
        );

        when(facultyService.findByDepartmentId(1L)).thenReturn(List.of(faculty));

        mockMvc.perform(get("/faculty")
                .param("departmentId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].departmentId").value(1));

        verify(facultyService).findByDepartmentId(1L);
    }

    @Test
    void shouldFindFacultyByStatus() throws Exception {
        Instant now = Instant.now();
        FacultyResponse faculty = new FacultyResponse(
            1L, "EMP001", "John", "Doe", "John Doe", "john@college.edu", "123",
            1L, "Computer Science", Designation.PROFESSOR, "AI", "ML Lab",
            LocalDate.of(2020, 1, 15), FacultyStatus.ON_LEAVE, now, now
        );

        when(facultyService.findByStatus(FacultyStatus.ON_LEAVE)).thenReturn(List.of(faculty));

        mockMvc.perform(get("/faculty")
                .param("status", "ON_LEAVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("ON_LEAVE"));

        verify(facultyService).findByStatus(FacultyStatus.ON_LEAVE);
    }

    @Test
    void shouldReturnEmptyListWhenNoFaculty() throws Exception {
        when(facultyService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/faculty"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(facultyService).findAll();
    }

    @Test
    void shouldFindFacultyById() throws Exception {
        Instant now = Instant.now();
        FacultyResponse response = new FacultyResponse(
            1L, "EMP001", "John", "Doe", "John Doe", "john@college.edu", "123",
            1L, "Computer Science", Designation.PROFESSOR, "AI", "ML Lab",
            LocalDate.of(2020, 1, 15), FacultyStatus.ACTIVE, now, now
        );

        when(facultyService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/faculty/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.employeeCode").value("EMP001"))
            .andExpect(jsonPath("$.fullName").value("John Doe"));

        verify(facultyService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenFacultyNotExists() throws Exception {
        when(facultyService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Faculty not found with id: 999"));

        mockMvc.perform(get("/faculty/999"))
            .andExpect(status().isNotFound());

        verify(facultyService).findById(999L);
    }

    @Test
    void shouldUpdateFaculty() throws Exception {
        FacultyRequest request = new FacultyRequest(
            "EMP001-UPD",
            "John Updated",
            "Doe Updated",
            "john.updated@college.edu",
            "9999999999",
            2L,
            Designation.ASSOCIATE_PROFESSOR,
            "Applied Mathematics",
            "Math Lab",
            LocalDate.of(2019, 1, 1),
            FacultyStatus.ON_LEAVE
        );

        Instant now = Instant.now();
        FacultyResponse response = new FacultyResponse(
            1L, "EMP001-UPD", "John Updated", "Doe Updated", "John Updated Doe Updated",
            "john.updated@college.edu", "9999999999", 2L, "Mathematics",
            Designation.ASSOCIATE_PROFESSOR, "Applied Mathematics", "Math Lab",
            LocalDate.of(2019, 1, 1), FacultyStatus.ON_LEAVE, now, now
        );

        when(facultyService.update(eq(1L), any(FacultyRequest.class))).thenReturn(response);

        mockMvc.perform(put("/faculty/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.employeeCode").value("EMP001-UPD"))
            .andExpect(jsonPath("$.fullName").value("John Updated Doe Updated"))
            .andExpect(jsonPath("$.designation").value("ASSOCIATE_PROFESSOR"))
            .andExpect(jsonPath("$.status").value("ON_LEAVE"));

        verify(facultyService).update(eq(1L), any(FacultyRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentFaculty() throws Exception {
        FacultyRequest request = new FacultyRequest(
            "EMP001", "John", "Doe", "john@college.edu", "123",
            1L, Designation.PROFESSOR, "AI", "ML Lab",
            LocalDate.of(2020, 1, 1), FacultyStatus.ACTIVE
        );

        when(facultyService.update(eq(999L), any(FacultyRequest.class)))
            .thenThrow(new ResourceNotFoundException("Faculty not found with id: 999"));

        mockMvc.perform(put("/faculty/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(facultyService).update(eq(999L), any(FacultyRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidData() throws Exception {
        FacultyRequest request = new FacultyRequest(
            "", "", "", "invalid", "123",
            1L, Designation.PROFESSOR, "AI", "ML Lab",
            LocalDate.of(2020, 1, 1), FacultyStatus.ACTIVE
        );

        mockMvc.perform(put("/faculty/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteFaculty() throws Exception {
        doNothing().when(facultyService).delete(1L);

        mockMvc.perform(delete("/faculty/1"))
            .andExpect(status().isNoContent());

        verify(facultyService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentFaculty() throws Exception {
        doThrow(new ResourceNotFoundException("Faculty not found with id: 999"))
            .when(facultyService).delete(999L);

        mockMvc.perform(delete("/faculty/999"))
            .andExpect(status().isNotFound());

        verify(facultyService).delete(999L);
    }
}
