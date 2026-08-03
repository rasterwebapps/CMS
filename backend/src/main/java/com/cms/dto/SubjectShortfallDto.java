package com.cms.dto;

public record SubjectShortfallDto(
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    double remainingShortfallHours
) {}
