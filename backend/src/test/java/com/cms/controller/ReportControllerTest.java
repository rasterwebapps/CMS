package com.cms.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.AttendanceAnalyticsReportResponse;
import com.cms.dto.ExamResultResponse;
import com.cms.dto.LabContinuousEvaluationResponse;
import com.cms.dto.LabUtilizationReportResponse;
import com.cms.dto.StudentPerformanceReportResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.ReportService;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void shouldGetLabUtilizationReport() throws Exception {
        LabUtilizationReportResponse response = new LabUtilizationReportResponse(5L, 20L, 4.0);

        when(reportService.getLabUtilizationReport()).thenReturn(response);

        mockMvc.perform(get("/reports/lab-utilization"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLabs").value(5))
            .andExpect(jsonPath("$.totalSchedules").value(20))
            .andExpect(jsonPath("$.averageSchedulesPerLab").value(4.0));

        verify(reportService).getLabUtilizationReport();
    }

    @Test
    void shouldGetStudentPerformanceReport() throws Exception {
        List<ExamResultResponse> examResults = List.of();
        List<LabContinuousEvaluationResponse> labEvaluations = List.of();
        StudentPerformanceReportResponse response = new StudentPerformanceReportResponse(
            1L, "John Doe", examResults, labEvaluations
        );

        when(reportService.getStudentPerformanceReport(1L)).thenReturn(response);

        mockMvc.perform(get("/reports/student-performance/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(1))
            .andExpect(jsonPath("$.studentName").value("John Doe"));

        verify(reportService).getStudentPerformanceReport(1L);
    }

    @Test
    void shouldReturnNotFoundForStudentPerformance() throws Exception {
        when(reportService.getStudentPerformanceReport(999L))
            .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

        mockMvc.perform(get("/reports/student-performance/999"))
            .andExpect(status().isNotFound());

        verify(reportService).getStudentPerformanceReport(999L);
    }

    @Test
    void shouldGetAttendanceAnalyticsReport() throws Exception {
        AttendanceAnalyticsReportResponse response = new AttendanceAnalyticsReportResponse(100L, 3500L);

        when(reportService.getAttendanceAnalyticsReport()).thenReturn(response);

        mockMvc.perform(get("/reports/attendance-analytics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalStudents").value(100))
            .andExpect(jsonPath("$.totalAttendanceRecords").value(3500));

        verify(reportService).getAttendanceAnalyticsReport();
    }
}
