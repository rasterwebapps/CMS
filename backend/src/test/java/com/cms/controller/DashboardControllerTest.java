package com.cms.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.DashboardSummaryResponse;
import com.cms.dto.DashboardTrendPoint;
import com.cms.dto.DashboardTrendsResponse;
import com.cms.service.DashboardService;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void shouldGetDashboardSummary() throws Exception {
        DashboardSummaryResponse response = new DashboardSummaryResponse(
            10L, 10L, 10L, 10L, 10L, 10L, 10L, 10L, 10L, 10L, 10L,
            Map.of("AVAILABLE", 6L, "IN_USE", 4L),
            Map.of("REQUESTED", 3L, "IN_PROGRESS", 2L, "COMPLETED", 5L),
            Map.of("ACTIVE", 10L),
            Map.of("PRESENT", 7L, "ABSENT", 3L),
            Map.of("NEW", 5L, "ENROLLED", 5L),
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(10000)
        );

        when(dashboardService.getSummary()).thenReturn(response);

        mockMvc.perform(get("/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalStudents").value(10))
            .andExpect(jsonPath("$.totalFaculty").value(10))
            .andExpect(jsonPath("$.totalDepartments").value(10))
            .andExpect(jsonPath("$.totalSubjects").value(10))
            .andExpect(jsonPath("$.totalLabs").value(10))
            .andExpect(jsonPath("$.totalEquipment").value(10))
            .andExpect(jsonPath("$.equipmentByStatus.AVAILABLE").value(6))
            .andExpect(jsonPath("$.maintenanceByStatus.COMPLETED").value(5))
            .andExpect(jsonPath("$.studentsByStatus.ACTIVE").value(10))
            .andExpect(jsonPath("$.attendanceByStatus.PRESENT").value(7))
            .andExpect(jsonPath("$.enquiryFunnel.NEW").value(5))
            .andExpect(jsonPath("$.feeCollectedThisMonth").value(50000))
            .andExpect(jsonPath("$.feeOutstanding").value(10000));

        verify(dashboardService).getSummary();
    }

    @Test
    void shouldGetDashboardTrends() throws Exception {
        DashboardTrendsResponse trendsResponse = new DashboardTrendsResponse(
            List.of(new DashboardTrendPoint("Jan 2025", 5L)),
            List.of(new DashboardTrendPoint("Jan 2025", 25000L))
        );

        when(dashboardService.getTrends()).thenReturn(trendsResponse);

        mockMvc.perform(get("/dashboard/trends"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enrolmentTrend[0].month").value("Jan 2025"))
            .andExpect(jsonPath("$.enrolmentTrend[0].value").value(5))
            .andExpect(jsonPath("$.feeCollectionTrend[0].value").value(25000));

        verify(dashboardService).getTrends();
    }
}

