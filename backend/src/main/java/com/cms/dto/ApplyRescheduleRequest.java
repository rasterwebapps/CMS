package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ApplyRescheduleRequest(
    @NotNull(message = "Date is required") LocalDate date,
    @NotNull(message = "Target date is required") LocalDate targetDate,
    @NotNull(message = "Period is required") Long periodId,
    @NotNull(message = "Venue is required") Long venueId
) {}
