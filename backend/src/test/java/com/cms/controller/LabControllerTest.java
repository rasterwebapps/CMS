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

import com.cms.dto.DepartmentResponse;
import com.cms.dto.LabInChargeAssignmentRequest;
import com.cms.dto.LabInChargeAssignmentResponse;
import com.cms.dto.LabRequest;
import com.cms.dto.LabResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.LabInChargeRole;
import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;
import com.cms.service.LabService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = LabController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LabService labService;

    @Test
    void shouldCreateLab() throws Exception {
        LabRequest request = new LabRequest(
            "Computer Lab 1",
            LabType.COMPUTER,
            1L,
            "Main Building",
            "101",
            30,
            LabStatus.ACTIVE
        );

        Instant now = Instant.now();
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        LabResponse response = new LabResponse(
            1L,
            "Computer Lab 1",
            LabType.COMPUTER,
            deptResponse,
            "Main Building",
            "101",
            30,
            LabStatus.ACTIVE,
            now,
            now
        );

        when(labService.create(any(LabRequest.class))).thenReturn(response);

        mockMvc.perform(post("/labs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Computer Lab 1"))
            .andExpect(jsonPath("$.labType").value("COMPUTER"))
            .andExpect(jsonPath("$.building").value("Main Building"))
            .andExpect(jsonPath("$.roomNumber").value("101"))
            .andExpect(jsonPath("$.capacity").value(30))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.department.id").value(1));

        verify(labService).create(any(LabRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        LabRequest request = new LabRequest(
            "",
            LabType.COMPUTER,
            1L,
            "Main Building",
            "101",
            30,
            LabStatus.ACTIVE
        );

        mockMvc.perform(post("/labs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenLabTypeIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Computer Lab 1",
                "departmentId": 1,
                "building": "Main Building",
                "roomNumber": "101",
                "capacity": 30,
                "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/labs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenDepartmentIdIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Computer Lab 1",
                "labType": "COMPUTER",
                "building": "Main Building",
                "roomNumber": "101",
                "capacity": 30,
                "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/labs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenStatusIsNull() throws Exception {
        String jsonRequest = """
            {
                "name": "Computer Lab 1",
                "labType": "COMPUTER",
                "departmentId": 1,
                "building": "Main Building",
                "roomNumber": "101",
                "capacity": 30
            }
            """;

        mockMvc.perform(post("/labs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCapacityIsZero() throws Exception {
        LabRequest request = new LabRequest(
            "Computer Lab 1",
            LabType.COMPUTER,
            1L,
            "Main Building",
            "101",
            0,
            LabStatus.ACTIVE
        );

        mockMvc.perform(post("/labs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCapacityIsNegative() throws Exception {
        LabRequest request = new LabRequest(
            "Computer Lab 1",
            LabType.COMPUTER,
            1L,
            "Main Building",
            "101",
            -5,
            LabStatus.ACTIVE
        );

        mockMvc.perform(post("/labs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllLabs() throws Exception {
        Instant now = Instant.now();
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        LabResponse lab1 = new LabResponse(
            1L, "Computer Lab 1", LabType.COMPUTER, deptResponse,
            "Main Building", "101", 30, LabStatus.ACTIVE, now, now
        );
        LabResponse lab2 = new LabResponse(
            2L, "Physics Lab", LabType.PHYSICS, deptResponse,
            "Science Building", "201", 25, LabStatus.ACTIVE, now, now
        );

        when(labService.findAll()).thenReturn(List.of(lab1, lab2));

        mockMvc.perform(get("/labs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Computer Lab 1"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("Physics Lab"));

        verify(labService).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoLabs() throws Exception {
        when(labService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/labs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(labService).findAll();
    }

    @Test
    void shouldFindLabById() throws Exception {
        Instant now = Instant.now();
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        LabResponse response = new LabResponse(
            1L,
            "Computer Lab 1",
            LabType.COMPUTER,
            deptResponse,
            "Main Building",
            "101",
            30,
            LabStatus.ACTIVE,
            now,
            now
        );

        when(labService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/labs/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Computer Lab 1"))
            .andExpect(jsonPath("$.labType").value("COMPUTER"));

        verify(labService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenLabNotExists() throws Exception {
        when(labService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Lab not found with id: 999"));

        mockMvc.perform(get("/labs/999"))
            .andExpect(status().isNotFound());

        verify(labService).findById(999L);
    }

    @Test
    void shouldFindLabsByDepartmentId() throws Exception {
        Instant now = Instant.now();
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        LabResponse lab1 = new LabResponse(
            1L, "Computer Lab 1", LabType.COMPUTER, deptResponse,
            "Main Building", "101", 30, LabStatus.ACTIVE, now, now
        );
        LabResponse lab2 = new LabResponse(
            2L, "Computer Lab 2", LabType.COMPUTER, deptResponse,
            "Main Building", "102", 25, LabStatus.ACTIVE, now, now
        );

        when(labService.findByDepartmentId(1L)).thenReturn(List.of(lab1, lab2));

        mockMvc.perform(get("/labs/department/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2));

        verify(labService).findByDepartmentId(1L);
    }

    @Test
    void shouldReturnNotFoundWhenFindingLabsByNonExistentDepartment() throws Exception {
        when(labService.findByDepartmentId(999L))
            .thenThrow(new ResourceNotFoundException("Department not found with id: 999"));

        mockMvc.perform(get("/labs/department/999"))
            .andExpect(status().isNotFound());

        verify(labService).findByDepartmentId(999L);
    }

    @Test
    void shouldUpdateLab() throws Exception {
        LabRequest request = new LabRequest(
            "Computer Lab Updated",
            LabType.ELECTRONICS,
            1L,
            "Science Building",
            "301",
            40,
            LabStatus.UNDER_MAINTENANCE
        );

        Instant now = Instant.now();
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        LabResponse response = new LabResponse(
            1L,
            "Computer Lab Updated",
            LabType.ELECTRONICS,
            deptResponse,
            "Science Building",
            "301",
            40,
            LabStatus.UNDER_MAINTENANCE,
            now,
            now
        );

        when(labService.update(eq(1L), any(LabRequest.class))).thenReturn(response);

        mockMvc.perform(put("/labs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Computer Lab Updated"))
            .andExpect(jsonPath("$.labType").value("ELECTRONICS"))
            .andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"));

        verify(labService).update(eq(1L), any(LabRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentLab() throws Exception {
        LabRequest request = new LabRequest(
            "Name",
            LabType.COMPUTER,
            1L,
            "Building",
            "101",
            30,
            LabStatus.ACTIVE
        );

        when(labService.update(eq(999L), any(LabRequest.class)))
            .thenThrow(new ResourceNotFoundException("Lab not found with id: 999"));

        mockMvc.perform(put("/labs/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(labService).update(eq(999L), any(LabRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingWithInvalidData() throws Exception {
        LabRequest request = new LabRequest(
            "",
            LabType.COMPUTER,
            1L,
            "Building",
            "101",
            30,
            LabStatus.ACTIVE
        );

        mockMvc.perform(put("/labs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteLab() throws Exception {
        doNothing().when(labService).delete(1L);

        mockMvc.perform(delete("/labs/1"))
            .andExpect(status().isNoContent());

        verify(labService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentLab() throws Exception {
        doThrow(new ResourceNotFoundException("Lab not found with id: 999"))
            .when(labService).delete(999L);

        mockMvc.perform(delete("/labs/999"))
            .andExpect(status().isNotFound());

        verify(labService).delete(999L);
    }

    @Test
    void shouldAssignInCharge() throws Exception {
        LabInChargeAssignmentRequest request = new LabInChargeAssignmentRequest(
            100L,
            "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE,
            LocalDate.of(2024, 1, 15)
        );

        Instant now = Instant.now();
        LabInChargeAssignmentResponse response = new LabInChargeAssignmentResponse(
            1L,
            1L,
            "Computer Lab 1",
            100L,
            "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE,
            LocalDate.of(2024, 1, 15),
            now,
            now
        );

        when(labService.assignInCharge(eq(1L), any(LabInChargeAssignmentRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/labs/1/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.labId").value(1))
            .andExpect(jsonPath("$.labName").value("Computer Lab 1"))
            .andExpect(jsonPath("$.assigneeId").value(100))
            .andExpect(jsonPath("$.assigneeName").value("Dr. Smith"))
            .andExpect(jsonPath("$.role").value("LAB_INCHARGE"));

        verify(labService).assignInCharge(eq(1L), any(LabInChargeAssignmentRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenAssigneeIdIsNull() throws Exception {
        String jsonRequest = """
            {
                "assigneeName": "Dr. Smith",
                "role": "LAB_INCHARGE",
                "assignedDate": "2024-01-15"
            }
            """;

        mockMvc.perform(post("/labs/1/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenAssigneeNameIsBlank() throws Exception {
        String jsonRequest = """
            {
                "assigneeId": 100,
                "assigneeName": "",
                "role": "LAB_INCHARGE",
                "assignedDate": "2024-01-15"
            }
            """;

        mockMvc.perform(post("/labs/1/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenRoleIsNull() throws Exception {
        String jsonRequest = """
            {
                "assigneeId": 100,
                "assigneeName": "Dr. Smith",
                "assignedDate": "2024-01-15"
            }
            """;

        mockMvc.perform(post("/labs/1/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenAssignedDateIsNull() throws Exception {
        String jsonRequest = """
            {
                "assigneeId": 100,
                "assigneeName": "Dr. Smith",
                "role": "LAB_INCHARGE"
            }
            """;

        mockMvc.perform(post("/labs/1/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenAssigningToNonExistentLab() throws Exception {
        LabInChargeAssignmentRequest request = new LabInChargeAssignmentRequest(
            100L,
            "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE,
            LocalDate.of(2024, 1, 15)
        );

        when(labService.assignInCharge(eq(999L), any(LabInChargeAssignmentRequest.class)))
            .thenThrow(new ResourceNotFoundException("Lab not found with id: 999"));

        mockMvc.perform(post("/labs/999/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(labService).assignInCharge(eq(999L), any(LabInChargeAssignmentRequest.class));
    }

    @Test
    void shouldRemoveAssignment() throws Exception {
        doNothing().when(labService).removeAssignment(1L, 10L);

        mockMvc.perform(delete("/labs/1/assignments/10"))
            .andExpect(status().isNoContent());

        verify(labService).removeAssignment(1L, 10L);
    }

    @Test
    void shouldReturnNotFoundWhenRemovingAssignmentFromNonExistentLab() throws Exception {
        doThrow(new ResourceNotFoundException("Lab not found with id: 999"))
            .when(labService).removeAssignment(999L, 10L);

        mockMvc.perform(delete("/labs/999/assignments/10"))
            .andExpect(status().isNotFound());

        verify(labService).removeAssignment(999L, 10L);
    }

    @Test
    void shouldReturnNotFoundWhenRemovingNonExistentAssignment() throws Exception {
        doThrow(new ResourceNotFoundException("Assignment not found with id: 999"))
            .when(labService).removeAssignment(1L, 999L);

        mockMvc.perform(delete("/labs/1/assignments/999"))
            .andExpect(status().isNotFound());

        verify(labService).removeAssignment(1L, 999L);
    }

    @Test
    void shouldFindAssignments() throws Exception {
        Instant now = Instant.now();
        LabInChargeAssignmentResponse assignment1 = new LabInChargeAssignmentResponse(
            1L, 1L, "Computer Lab 1", 100L, "Dr. Smith",
            LabInChargeRole.LAB_INCHARGE, LocalDate.of(2024, 1, 15), now, now
        );
        LabInChargeAssignmentResponse assignment2 = new LabInChargeAssignmentResponse(
            2L, 1L, "Computer Lab 1", 101L, "Mr. Brown",
            LabInChargeRole.TECHNICIAN, LocalDate.of(2024, 2, 1), now, now
        );

        when(labService.findAssignmentsByLabId(1L)).thenReturn(List.of(assignment1, assignment2));

        mockMvc.perform(get("/labs/1/assignments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].assigneeName").value("Dr. Smith"))
            .andExpect(jsonPath("$[0].role").value("LAB_INCHARGE"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].assigneeName").value("Mr. Brown"))
            .andExpect(jsonPath("$[1].role").value("TECHNICIAN"));

        verify(labService).findAssignmentsByLabId(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoAssignments() throws Exception {
        when(labService.findAssignmentsByLabId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/labs/1/assignments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(labService).findAssignmentsByLabId(1L);
    }

    @Test
    void shouldReturnNotFoundWhenFindingAssignmentsForNonExistentLab() throws Exception {
        when(labService.findAssignmentsByLabId(999L))
            .thenThrow(new ResourceNotFoundException("Lab not found with id: 999"));

        mockMvc.perform(get("/labs/999/assignments"))
            .andExpect(status().isNotFound());

        verify(labService).findAssignmentsByLabId(999L);
    }

    @Test
    void shouldAllowNullCapacity() throws Exception {
        LabRequest request = new LabRequest(
            "Computer Lab 1",
            LabType.COMPUTER,
            1L,
            "Main Building",
            "101",
            null,
            LabStatus.ACTIVE
        );

        Instant now = Instant.now();
        DepartmentResponse deptResponse = new DepartmentResponse(
            1L, "Computer Science", "CS", "CS Department", "Dr. John", now, now
        );
        LabResponse response = new LabResponse(
            1L, "Computer Lab 1", LabType.COMPUTER, deptResponse,
            "Main Building", "101", null, LabStatus.ACTIVE, now, now
        );

        when(labService.create(any(LabRequest.class))).thenReturn(response);

        mockMvc.perform(post("/labs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.capacity").isEmpty());

        verify(labService).create(any(LabRequest.class));
    }
}
