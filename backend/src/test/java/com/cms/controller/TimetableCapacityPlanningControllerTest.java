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

import com.cms.dto.CohortAutoPlanSummaryResponse;
import com.cms.dto.FacultyWorkloadReportResponse;
import com.cms.dto.FacultyWorkloadRow;
import com.cms.dto.RoomInventoryRowResponse;
import com.cms.dto.TermCapacityOverviewResponse;
import com.cms.model.enums.PlanningBasis;
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

    @MockitoBean
    private com.cms.service.TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;

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

    @Test
    void shouldGetTermOverview() throws Exception {
        CohortAutoPlanSummaryResponse row = new CohortAutoPlanSummaryResponse(
            5L, "BSc Nursing 2026 - Sem 1", 1, 60L, false, true, null, List.of(), List.of(), true, null, 0, 0);
        RoomInventoryRowResponse roomRow = new RoomInventoryRowResponse(1L, "Room 101", "CLASSROOM", 60, null, 0L, 0, 0.0);
        TermCapacityOverviewResponse overview = new TermCapacityOverviewResponse(
            10L, true, 60, 60, null, List.of(row), List.of(roomRow), true, null, true, null, false, null,
            List.of(), List.of());
        when(timetableCapacityPlanningService.getTermOverview(10L, PlanningBasis.ENROLLED)).thenReturn(overview);

        mockMvc.perform(get("/timetables/capacity-plan/term-overview")
                .param("termInstanceId", "10")
                .param("planningBasis", "ENROLLED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.theorySufficient").value(true))
            .andExpect(jsonPath("$.cohorts[0].cohortId").value(5))
            .andExpect(jsonPath("$.cohorts[0].hasCommittedAllocation").value(false))
            .andExpect(jsonPath("$.roomInventory[0].roomType").value("CLASSROOM"));
    }

    /** Confirms Capacity Planner's Venue Utilization panel is wired to the real weekly-demand
     *  over/tight classification -- deliberately a distinct endpoint from {@link #shouldGetTermOverview}
     *  since {@code getPlan} (single-cohort) never computes this, and the panel must never gate
     *  "Rebalance now" on its own unrelated placed-schedule-cell utilization figures instead. */
    @Test
    void shouldGetVenueCapacity() throws Exception {
        com.cms.dto.VenueTightCapacity tight = new com.cms.dto.VenueTightCapacity(
            50L, "CLINICAL", "Ward 1 - Medical", 30, 40, 40, 100.0, List.of("Nursing Foundation I"), List.of(900L));
        com.cms.dto.LabClinicalVenueCapacityResult result =
            new com.cms.dto.LabClinicalVenueCapacityResult(List.of(), List.of(tight));
        when(timetableCapacityPlanningService.computeLabClinicalVenueCapacity(10L, PlanningBasis.SANCTIONED)).thenReturn(result);

        mockMvc.perform(get("/timetables/capacity-plan/venue-capacity")
                .param("termInstanceId", "10")
                .param("planningBasis", "SANCTIONED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overCapacityVenues").isEmpty())
            .andExpect(jsonPath("$.tightCapacityVenues[0].venueId").value(50))
            .andExpect(jsonPath("$.tightCapacityVenues[0].venueName").value("Ward 1 - Medical"));
    }
}
