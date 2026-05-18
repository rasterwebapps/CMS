package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.BulkRollNumberAssignmentRequest;
import com.cms.dto.BulkRollNumberItem;
import com.cms.dto.GenerateRollNumbersRequest;
import com.cms.dto.ProgramTransferAnalysis;
import com.cms.dto.ProgramTransferRecord;
import com.cms.dto.ProgramTransferRequest;
import com.cms.dto.RollNumberAssignment;
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

    @MockitoBean
    private com.cms.service.RollNumberGeneratorService rollNumberGeneratorService;

    @Test
    void shouldCreateStudent() throws Exception {
        StudentRequest request = new StudentRequest(
            "CS2024001", "John", "Doe", "john@college.edu", "1234567890",
            1L, null, null, 1, LocalDate.of(2024, 6, 1), "Batch-A", StudentStatus.ACTIVE,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
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
    void shouldAllowNullRollNumberOnCreate() throws Exception {
        StudentRequest request = new StudentRequest(
            null, "John", "Doe", "john@college.edu", "1234567890",
            1L, null, null, 1, LocalDate.of(2024, 6, 1), null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        );

        StudentResponse response = createStudentResponse(1L, null, "John", "Doe");

        when(studentService.create(any(StudentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
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
            1L, null, null, 2, LocalDate.of(2024, 6, 1), "Batch-B", StudentStatus.ACTIVE,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
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

    @Test
    void shouldGenerateRollNumbers() throws Exception {
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
            Arrays.asList(1L, 2L),
            1L,
            2026
        );

        List<RollNumberAssignment> assignments = Arrays.asList(
            new RollNumberAssignment("959652026001", 1L, "Alice Brown"),
            new RollNumberAssignment("959652026002", 2L, "Bob Anderson")
        );

        when(rollNumberGeneratorService.generateAndAssignRollNumbers(any(GenerateRollNumbersRequest.class)))
            .thenReturn(assignments);

        mockMvc.perform(post("/students/generate-roll-numbers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].rollNumber").value("959652026001"))
            .andExpect(jsonPath("$[0].studentName").value("Alice Brown"))
            .andExpect(jsonPath("$[1].rollNumber").value("959652026002"));

        verify(rollNumberGeneratorService).generateAndAssignRollNumbers(any(GenerateRollNumbersRequest.class));
    }

    @Test
    void shouldPreviewRollNumbers() throws Exception {
        GenerateRollNumbersRequest request = new GenerateRollNumbersRequest(
            Arrays.asList(1L, 2L),
            1L,
            2026
        );

        List<RollNumberAssignment> preview = Arrays.asList(
            new RollNumberAssignment("959652026001", 1L, "Alice Brown"),
            new RollNumberAssignment("959652026002", 2L, "Bob Anderson")
        );

        when(rollNumberGeneratorService.previewRollNumbers(any(GenerateRollNumbersRequest.class)))
            .thenReturn(preview);

        mockMvc.perform(post("/students/preview-roll-numbers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].rollNumber").value("959652026001"))
            .andExpect(jsonPath("$[1].studentId").value(2));

        verify(rollNumberGeneratorService).previewRollNumbers(any(GenerateRollNumbersRequest.class));
    }

    @Test
    void shouldFindStudentsByStatus() throws Exception {
        StudentResponse response = createStudentResponse(1L, "CS2024001", "John", "Doe");
        when(studentService.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(response));

        mockMvc.perform(get("/students").param("status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(studentService).findByStatus(StudentStatus.ACTIVE);
    }

    @Test
    void shouldFindStudentsByLabBatch() throws Exception {
        StudentResponse response = createStudentResponse(1L, "CS2024001", "John", "Doe");
        when(studentService.findByLabBatch("Batch-A")).thenReturn(List.of(response));

        mockMvc.perform(get("/students").param("labBatch", "Batch-A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(studentService).findByLabBatch("Batch-A");
    }

    @Test
    void shouldFindStudentsWithoutRollNumber() throws Exception {
        StudentResponse response = createStudentResponse(1L, null, "John", "Doe");
        when(studentService.findStudentsWithoutRollNumber(1L, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/students/without-roll-number").param("courseId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(studentService).findStudentsWithoutRollNumber(1L, null);
    }

    @Test
    void shouldAssignRollNumberToStudent() throws Exception {
        StudentResponse response = createStudentResponse(1L, "CS2024001", "John", "Doe");
        when(studentService.assignRollNumber(1L, "CS2024001")).thenReturn(response);

        mockMvc.perform(patch("/students/1/roll-number")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rollNumber\": \"CS2024001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rollNumber").value("CS2024001"));

        verify(studentService).assignRollNumber(1L, "CS2024001");
    }

    @Test
    void shouldBulkAssignRollNumbers() throws Exception {
        StudentResponse response = createStudentResponse(1L, "CS2024001", "John", "Doe");
        when(studentService.bulkAssignRollNumbers(any())).thenReturn(List.of(response));

        BulkRollNumberAssignmentRequest request = new BulkRollNumberAssignmentRequest(
            List.of(new BulkRollNumberItem(1L, "CS2024001")));

        mockMvc.perform(post("/students/bulk-assign-roll-numbers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(studentService).bulkAssignRollNumbers(any());
    }

    @Test
    void shouldAnalyzeProgramTransfer() throws Exception {
        ProgramTransferAnalysis analysis = new ProgramTransferAnalysis(
            1L, "John Doe", 1L, "B.Tech CS", 2L, "B.Tech IT",
            List.of(), List.of(), List.of());
        when(studentService.analyzeProgramTransfer(1L, 2L)).thenReturn(analysis);

        mockMvc.perform(get("/students/1/program-transfer-analysis").param("newProgramId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(1));

        verify(studentService).analyzeProgramTransfer(1L, 2L);
    }

    @Test
    void shouldExecuteProgramTransfer() throws Exception {
        ProgramTransferRecord record = new ProgramTransferRecord(
            1L, 1L, "John Doe", 1L, "B.Tech CS", 2L, "B.Tech IT",
            Instant.now(), "admin", true, "Transfer");
        when(studentService.executeProgramTransfer(eq(1L), any(ProgramTransferRequest.class))).thenReturn(record);

        ProgramTransferRequest request = new ProgramTransferRequest(2L, null, true, "Transfer");

        mockMvc.perform(post("/students/1/program-transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.newProgramId").value(2));

        verify(studentService).executeProgramTransfer(eq(1L), any(ProgramTransferRequest.class));
    }

    @Test
    void shouldGetTransferHistory() throws Exception {
        ProgramTransferRecord record = new ProgramTransferRecord(
            1L, 1L, "John Doe", 1L, "B.Tech CS", 2L, "B.Tech IT",
            Instant.now(), "admin", true, "Transfer");
        when(studentService.getTransferHistory(1L)).thenReturn(List.of(record));

        mockMvc.perform(get("/students/1/program-transfers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(studentService).getTransferHistory(1L);
    }

    private StudentResponse createStudentResponse(Long id, String rollNumber, String firstName, String lastName) {
        Instant now = Instant.now();
        return new StudentResponse(
            id, rollNumber, firstName, lastName, firstName + " " + lastName,
            firstName.toLowerCase() + "@college.edu", "1234567890", 1L, "B.Tech Computer Science",
            null, null, null, null,
            1, LocalDate.of(2024, 6, 1), "Batch-A", StudentStatus.ACTIVE,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null,
            now, now
        );
    }
}
