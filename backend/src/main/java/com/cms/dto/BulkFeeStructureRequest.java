package com.cms.dto;

import java.util.List;

import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.Gender;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BulkFeeStructureRequest(
    @NotNull(message = "Program ID is required")
    Long programId,

    @NotNull(message = "Academic Year ID is required")
    Long academicYearId,

    Long courseId,

    @NotNull(message = "Quota is required")
    AdmissionQuota quota,

    @NotNull(message = "Fee state ID is required")
    Long feeStateId,

    @NotNull(message = "Gender is required")
    Gender gender,

    @NotNull(message = "Fee items are required")
    @Size(min = 1, message = "At least one fee item is required")
    List<@Valid FeeStructureItemRequest> items
) {}
