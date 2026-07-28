package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record LogProgressRequest(
    @NotNull(message = "Class schedule ID is required")
    Long classScheduleId,

    @NotNull(message = "Occurrence date is required")
    LocalDate occurrenceDate,

    List<Long> unitIds,

    String remarks
) {}
