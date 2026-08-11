package com.cms.controller;

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

import com.cms.dto.FacultyWorkloadReportResponse;
import com.cms.dto.FacultyWorkloadRow;
import com.cms.service.FacultyWorkloadCapacityService;
import com.cms.service.TimetableCapacityPlanningService;

@WebMvcTest(controllers = TimetableCapacityPlanningController.class)
@AutoConfigureMockMvc(addFilters = false)
class TimetableCapacityPlanningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimetableCapacityPlanningService timetableCapacityPlanningService;

    @MockitoBean
    private FacultyWorkloadCapacityService facultyWorkloadCapacityService;

    @Test
    void shouldGetFacultyWorkloadReport() throws Exception {
        FacultyWorkloadRow row = new FacultyWorkloadRow(
            1L, "Jane Doe", "Professor",
            18.0, 16.0, 4.0,
            true, 20.0, 16.0,
            true, false);
        FacultyWorkloadReportResponse response = new FacultyWorkloadReportResponse(
            10L, List.of(row), 18.0, 16.0, 16.0, 0);

        when(facultyWorkloadCapacityService.getTermWorkloadReport(10L)).thenReturn(response);

        mockMvc.perform(get("/timetables/capacity-plan/faculty-workload").param("termInstanceId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.termInstanceId").value(10))
            .andExpect(jsonPath("$.rows[0].facultyName").value("Jane Doe"))
            .andExpect(jsonPath("$.rows[0].overDemand").value(true))
            .andExpect(jsonPath("$.rows[0].overCommitted").value(false))
            .andExpect(jsonPath("$.unconfiguredFacultyCount").value(0));
    }
}
