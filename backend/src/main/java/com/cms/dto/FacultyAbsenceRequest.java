package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record FacultyAbsenceRequest(
    @NotNull(message = "Faculty ID is required")
    Long facultyId,

    @NotNull(message = "Absence date is required")
    LocalDate absenceDate,

    String reason
) {}
