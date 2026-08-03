package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.PortionShortfallResponse;
import com.cms.dto.SyllabusUnitPlanResponse;
import com.cms.dto.UnitVarianceDto;
import com.cms.service.PortionBlueprintService;
import com.cms.service.PortionShortfallService;

@RestController
@RequestMapping("/portion-blueprint")
public class PortionBlueprintController {

    private final PortionBlueprintService portionBlueprintService;
    private final PortionShortfallService portionShortfallService;

    public PortionBlueprintController(PortionBlueprintService portionBlueprintService,
                                       PortionShortfallService portionShortfallService) {
        this.portionBlueprintService = portionBlueprintService;
        this.portionShortfallService = portionShortfallService;
    }

    @PostMapping("/course-offerings/{courseOfferingId}/generate")
    @PreAuthorize("@perm.has('PORTION_BLUEPRINT_MANAGE')")
    public ResponseEntity<List<SyllabusUnitPlanResponse>> generateBlueprint(@PathVariable Long courseOfferingId) {
        return ResponseEntity.ok(portionBlueprintService.generateBlueprint(courseOfferingId));
    }

    @GetMapping("/course-offerings/{courseOfferingId}")
    @PreAuthorize("@perm.has('PROGRESS_REPORT_VIEW') or @perm.has('PROGRESS_LOG_CREATE')")
    public ResponseEntity<List<SyllabusUnitPlanResponse>> getBlueprint(@PathVariable Long courseOfferingId) {
        return ResponseEntity.ok(portionBlueprintService.getBlueprint(courseOfferingId));
    }

    @GetMapping("/course-offerings/{courseOfferingId}/projection")
    @PreAuthorize("@perm.has('PROGRESS_REPORT_VIEW') or @perm.has('PROGRESS_LOG_CREATE')")
    public ResponseEntity<List<UnitVarianceDto>> getProjection(@PathVariable Long courseOfferingId) {
        return ResponseEntity.ok(portionBlueprintService.getProjection(courseOfferingId));
    }

    @GetMapping("/shortfall")
    @PreAuthorize("@perm.has('PROGRESS_REPORT_VIEW')")
    public ResponseEntity<PortionShortfallResponse> checkShortfall(
            @RequestParam Long termInstanceId, @RequestParam Long cohortId) {
        return ResponseEntity.ok(portionShortfallService.checkShortfall(termInstanceId, cohortId));
    }
}
