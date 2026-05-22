package com.cms.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcademicYearRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    Boolean isCurrent,

    @Valid
    List<CohortSeatAllocationRequest> cohortSeatAllocations
) {
    public AcademicYearRequest(String name, LocalDate startDate, LocalDate endDate, Boolean isCurrent) {
        this(name, startDate, endDate, isCurrent, List.of());
    }
}
