package com.cms.dto;

import java.time.LocalDate;

public record SyllabusUnitPlanResponse(
    Long unitId,
    Integer unitNumber,
    String title,
    LocalDate plannedCompletionDate,
    Integer plannedCumulativeHours,
    Integer sequenceIndex
) {}
