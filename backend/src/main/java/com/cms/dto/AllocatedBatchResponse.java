package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

public record AllocatedBatchResponse(
    Long batchId,
    Long courseOfferingId,
    String subjectName,
    ClassSessionType sessionType,
    Long venueId,
    String venueName,
    Integer venueCapacity,
    String batchName,
    Integer plannedSize,
    Boolean isActive,
    Long cohortSectionId,
    String cohortSectionLabel
) {}
