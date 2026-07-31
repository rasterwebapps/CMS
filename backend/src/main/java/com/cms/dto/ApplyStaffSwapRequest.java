package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ApplyStaffSwapRequest(
    @NotNull(message = "Target class schedule ID is required")
    Long targetClassScheduleId,

    @NotNull(message = "Date is required")
    LocalDate date
) {}
