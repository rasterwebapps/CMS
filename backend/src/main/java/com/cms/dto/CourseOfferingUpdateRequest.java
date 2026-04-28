package com.cms.dto;

public record CourseOfferingUpdateRequest(
    Long facultyId,
    String sectionLabel
) {}
