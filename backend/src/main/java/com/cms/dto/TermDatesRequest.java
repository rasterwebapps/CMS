package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record TermDatesRequest(
    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate
) {}
