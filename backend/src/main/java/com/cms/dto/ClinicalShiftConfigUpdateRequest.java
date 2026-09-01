package com.cms.dto;

import jakarta.validation.constraints.Min;

public record ClinicalShiftConfigUpdateRequest(
    @Min(value = 1, message = "Clinical shift duration must be at least 1 minute")
    Integer clinicalShiftDurationMinutes,

    @Min(value = 0, message = "Travel buffer cannot be negative")
    Integer clinicalTravelBufferMinutes
) {}
