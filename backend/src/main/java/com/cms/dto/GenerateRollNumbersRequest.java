package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GenerateRollNumbersRequest(
    @NotNull Long courseId,
    @NotEmpty List<Long> studentIds,
    @NotNull @Positive Integer academicYear
) {}
