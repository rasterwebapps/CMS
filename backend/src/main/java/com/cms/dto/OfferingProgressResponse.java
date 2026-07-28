package com.cms.dto;

import java.util.List;

public record OfferingProgressResponse(
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    int totalUnits,
    int coveredUnitCount,
    double percentComplete,
    List<UnitProgressDto> units
) {}
