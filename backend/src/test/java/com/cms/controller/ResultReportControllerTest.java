package com.cms.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.CourseStatsDto;
import com.cms.dto.SemesterSummaryDto;
import com.cms.dto.StudentResultSheetDto;
import com.cms.model.enums.ResultStatus;
import com.cms.service.ResultReportService;

@WebMvcTest(controllers = ResultReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResultReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResultReportService resultReportService;

    @Test
    void getResultSheet_returns200() throws Exception {
        StudentResultSheetDto sheet = new StudentResultSheetDto(
            100L, "Alice Smith", "ROLL-001", 5L, "2026-2027 ODD",
            List.of(), new BigDecimal("100"), new BigDecimal("75"),
            new BigDecimal("75.00"), ResultStatus.PASS);

        when(resultReportService.getResultSheet(10L)).thenReturn(sheet);

        mockMvc.perform(get("/api/result-reports/result-sheet/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(100));
    }

    @Test
    void getSummary_returns200() throws Exception {
        SemesterSummaryDto summary = new SemesterSummaryDto(
            5L, "2026-2027 ODD", 1L, "BSCN-2026-2030",
            30, 25, 5, new BigDecimal("80.00"));

        when(resultReportService.getSummaryByTermInstance(5L)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/result-reports/summary").param("termInstanceId", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].cohortCode").value("BSCN-2026-2030"));
    }

    @Test
    void getCourseStats_returns200() throws Exception {
        CourseStatsDto stats = new CourseStatsDto(
            20L, "Mathematics", "MATH101", 5L, "2026-2027 ODD",
            30, 28, 1, 1, new BigDecimal("72.00"), new BigDecimal("100"));

        when(resultReportService.getCourseStatsByTermInstance(5L)).thenReturn(List.of(stats));

        mockMvc.perform(get("/api/result-reports/course-stats").param("termInstanceId", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].subjectCode").value("MATH101"));
    }
}
