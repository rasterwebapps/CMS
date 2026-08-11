package com.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CapacityPlanResponse;
import com.cms.dto.FacultyWorkloadReportResponse;
import com.cms.model.enums.PlanningBasis;
import com.cms.service.FacultyWorkloadCapacityService;
import com.cms.service.TimetableCapacityPlanningService;

@RestController
@RequestMapping("/timetables/capacity-plan")
public class TimetableCapacityPlanningController {

    private final TimetableCapacityPlanningService timetableCapacityPlanningService;
    private final FacultyWorkloadCapacityService facultyWorkloadCapacityService;

    public TimetableCapacityPlanningController(TimetableCapacityPlanningService timetableCapacityPlanningService,
                                                 FacultyWorkloadCapacityService facultyWorkloadCapacityService) {
        this.timetableCapacityPlanningService = timetableCapacityPlanningService;
        this.facultyWorkloadCapacityService = facultyWorkloadCapacityService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_VIEW')")
    public ResponseEntity<CapacityPlanResponse> getPlan(@RequestParam Long termInstanceId,
                                                          @RequestParam Long cohortId,
                                                          @RequestParam(required = false) PlanningBasis planningBasis) {
        return ResponseEntity.ok(timetableCapacityPlanningService.getPlan(termInstanceId, cohortId, planningBasis));
    }

    /** Same permission as {@link #getPlan} — a second read view within the same already-gated
     *  Capacity Planner screen, not a new distinct operation. */
    @GetMapping("/faculty-workload")
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_VIEW')")
    public ResponseEntity<FacultyWorkloadReportResponse> getFacultyWorkload(@RequestParam Long termInstanceId) {
        return ResponseEntity.ok(facultyWorkloadCapacityService.getTermWorkloadReport(termInstanceId));
    }
}
