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

import java.math.BigDecimal;
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

import com.cms.dto.AttendanceReportResponse;
import com.cms.dto.AttendanceRequest;
import com.cms.dto.AttendanceResponse;
import com.cms.dto.BulkAttendanceRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.AttendanceStatus;
import com.cms.model.enums.AttendanceType;
import com.cms.service.AttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AttendanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AttendanceService attendanceService;

    @Test
    void shouldMarkAttendance() throws Exception {
        AttendanceRequest request = new AttendanceRequest(
            1L, 1L, LocalDate.now(), AttendanceStatus.PRESENT, AttendanceType.THEORY, null
        );

        AttendanceResponse response = createResponse(1L, AttendanceStatus.PRESENT);

        when(attendanceService.markAttendance(any(AttendanceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/attendance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PRESENT"));

        verify(attendanceService).markAttendance(any(AttendanceRequest.class));
    }

    @Test
    void shouldMarkBulkAttendance() throws Exception {
        BulkAttendanceRequest.StudentAttendance sa = new BulkAttendanceRequest.StudentAttendance(
            1L, AttendanceStatus.PRESENT, null
        );
        BulkAttendanceRequest request = new BulkAttendanceRequest(
            1L, LocalDate.now(), AttendanceType.THEORY, List.of(sa)
        );

        AttendanceResponse response = createResponse(1L, AttendanceStatus.PRESENT);

        when(attendanceService.markBulkAttendance(any(BulkAttendanceRequest.class)))
            .thenReturn(List.of(response));

        mockMvc.perform(post("/attendance/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.length()").value(1));

        verify(attendanceService).markBulkAttendance(any(BulkAttendanceRequest.class));
    }

    @Test
    void shouldFindAttendanceByStudentId() throws Exception {
        AttendanceResponse response = createResponse(1L, AttendanceStatus.PRESENT);

        when(attendanceService.findByStudentId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/attendance").param("studentId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(attendanceService).findByStudentId(1L);
    }

    @Test
    void shouldFindAttendanceByCourseId() throws Exception {
        AttendanceResponse response = createResponse(1L, AttendanceStatus.PRESENT);

        when(attendanceService.findByCourseId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/attendance").param("courseId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(attendanceService).findByCourseId(1L);
    }

    @Test
    void shouldFindAttendanceByStudentIdAndCourseId() throws Exception {
        AttendanceResponse response = createResponse(1L, AttendanceStatus.PRESENT);

        when(attendanceService.findByStudentIdAndCourseId(1L, 1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/attendance").param("studentId", "1").param("courseId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(attendanceService).findByStudentIdAndCourseId(1L, 1L);
    }

    @Test
    void shouldGetAttendanceReport() throws Exception {
        AttendanceReportResponse report = new AttendanceReportResponse(
            1L, "John Doe", "CS2024001", 1L, "Data Structures", "CS201",
            10, 8, new BigDecimal("80.00"), false
        );

        when(attendanceService.getAttendanceReport(1L, 1L)).thenReturn(report);

        mockMvc.perform(get("/attendance/reports")
                .param("studentId", "1")
                .param("courseId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attendancePercentage").value(80.00));

        verify(attendanceService).getAttendanceReport(1L, 1L);
    }

    @Test
    void shouldGetLowAttendanceAlerts() throws Exception {
        AttendanceReportResponse alert = new AttendanceReportResponse(
            1L, "John Doe", "CS2024001", 1L, "Data Structures", "CS201",
            10, 6, new BigDecimal("60.00"), true
        );

        when(attendanceService.getLowAttendanceAlerts(1L)).thenReturn(List.of(alert));

        mockMvc.perform(get("/attendance/alerts").param("courseId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].lowAttendance").value(true));

        verify(attendanceService).getLowAttendanceAlerts(1L);
    }

    @Test
    void shouldUpdateAttendance() throws Exception {
        AttendanceRequest request = new AttendanceRequest(
            1L, 1L, LocalDate.now(), AttendanceStatus.ABSENT, AttendanceType.THEORY, "Was sick"
        );

        AttendanceResponse response = createResponse(1L, AttendanceStatus.ABSENT);

        when(attendanceService.update(eq(1L), any(AttendanceRequest.class))).thenReturn(response);

        mockMvc.perform(put("/attendance/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ABSENT"));

        verify(attendanceService).update(eq(1L), any(AttendanceRequest.class));
    }

    @Test
    void shouldDeleteAttendance() throws Exception {
        doNothing().when(attendanceService).delete(1L);

        mockMvc.perform(delete("/attendance/1"))
            .andExpect(status().isNoContent());

        verify(attendanceService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentAttendance() throws Exception {
        doThrow(new ResourceNotFoundException("Attendance not found with id: 999"))
            .when(attendanceService).delete(999L);

        mockMvc.perform(delete("/attendance/999"))
            .andExpect(status().isNotFound());

        verify(attendanceService).delete(999L);
    }

    private AttendanceResponse createResponse(Long id, AttendanceStatus status) {
        Instant now = Instant.now();
        return new AttendanceResponse(
            id, 1L, "John Doe", "CS2024001", 1L, "Data Structures", "CS201",
            LocalDate.now(), status, AttendanceType.THEORY, null,
            null, null, now, now
        );
    }
}
