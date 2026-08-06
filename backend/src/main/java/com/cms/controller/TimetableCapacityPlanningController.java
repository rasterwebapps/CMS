package com.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CapacityPlanResponse;
import com.cms.model.enums.PlanningBasis;
import com.cms.service.TimetableCapacityPlanningService;

@RestController
@RequestMapping("/timetables/capacity-plan")
public class TimetableCapacityPlanningController {

    private final TimetableCapacityPlanningService timetableCapacityPlanningService;

    public TimetableCapacityPlanningController(TimetableCapacityPlanningService timetableCapacityPlanningService) {
        this.timetableCapacityPlanningService = timetableCapacityPlanningService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('TIMETABLE_CAPACITY_PLANNER_VIEW')")
    public ResponseEntity<CapacityPlanResponse> getPlan(@RequestParam Long termInstanceId,
                                                          @RequestParam Long cohortId,
                                                          @RequestParam(required = false) PlanningBasis planningBasis) {
        return ResponseEntity.ok(timetableCapacityPlanningService.getPlan(termInstanceId, cohortId, planningBasis));
    }
}
