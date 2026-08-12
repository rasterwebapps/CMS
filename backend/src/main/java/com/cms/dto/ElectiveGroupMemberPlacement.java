package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

import jakarta.validation.constraints.NotNull;

public record ElectiveGroupMemberPlacement(
    @NotNull(message = "Course offering is required") Long courseOfferingId,
    @NotNull(message = "Session type is required") ClassSessionType sessionType,

    /** Required for LAB/CLINICAL; ignored for THEORY. */
    Long batchId,

    /** THEORY only, required when the cohort has an active committed room allocation. */
    Long cohortSectionId
) {}
