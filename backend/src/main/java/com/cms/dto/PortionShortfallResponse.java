package com.cms.dto;

import java.util.List;

/** Cohort-semester-wide roll-up: sum of each non-elective subject's own remaining shortfall hours
 *  (from {@code PortionBlueprintService.remainingShortfallHours}) compared against that
 *  cohort-semester's existing Capacity Planner buffer -- reused, not recomputed. */
public record PortionShortfallResponse(
    Long termInstanceId,
    Long cohortId,
    double bufferHours,
    double totalShortfallHours,
    boolean atRisk,
    List<SubjectShortfallDto> subjects
) {}
