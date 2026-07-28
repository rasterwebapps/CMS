package com.cms.dto;

public record SubjectProgressSummaryDto(
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    int totalUnits,
    int coveredUnitCount,
    double percentComplete
) {}
