package com.cms.dto;

public record CohortSectionResponse(
    Long id,
    String sectionLabel,
    Long classroomId,
    String classroomName,
    Integer classroomCapacity,
    Integer plannedSize,
    Boolean isActive
) {}
