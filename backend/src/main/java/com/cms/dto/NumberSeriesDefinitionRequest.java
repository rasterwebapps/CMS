package com.cms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NumberSeriesDefinitionRequest(

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Series code must be uppercase letters, digits and underscores only")
    String seriesCode,

    @NotBlank
    @Size(max = 100)
    String seriesName,

    @NotNull
    @Pattern(regexp = "^(NONE|CALENDAR_DAY|CALENDAR_MONTH|CALENDAR_YEAR|FINANCIAL_MONTH|FINANCIAL_YEAR|ACADEMIC_YEAR|COURSE|ACADEMIC_YEAR_COURSE)$",
             message = "Invalid scope type")
    String scopeType,

    @Size(max = 30)
    String prefix,

    @Size(max = 5)
    String separator,

    @NotNull
    @Min(1) @Max(10)
    Integer sequencePadding,

    String description
) {}
