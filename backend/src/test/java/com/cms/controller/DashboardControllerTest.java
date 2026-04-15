package com.cms.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.DashboardSummaryResponse;
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
            Map.of("PRESENT", 7L, "ABSENT", 3L)
        );

        when(dashboardService.getSummary()).thenReturn(response);

        mockMvc.perform(get("/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalStudents").value(10))
            .andExpect(jsonPath("$.totalFaculty").value(10))
            .andExpect(jsonPath("$.totalDepartments").value(10))
            .andExpect(jsonPath("$.totalCourses").value(10))
            .andExpect(jsonPath("$.totalLabs").value(10))
            .andExpect(jsonPath("$.totalEquipment").value(10))
            .andExpect(jsonPath("$.equipmentByStatus.AVAILABLE").value(6))
            .andExpect(jsonPath("$.maintenanceByStatus.COMPLETED").value(5))
            .andExpect(jsonPath("$.studentsByStatus.ACTIVE").value(10))
            .andExpect(jsonPath("$.attendanceByStatus.PRESENT").value(7));

        verify(dashboardService).getSummary();
    }
}

