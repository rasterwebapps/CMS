package com.cms.dto;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;

public record SwapRequest(

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Period is required")
    Long periodId
) {}
