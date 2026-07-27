package com.cms.dto;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;

/** Exactly one of periodId/labSlotId must be supplied, matching the source session's
 *  sessionType (THEORY -> periodId, LAB -> labSlotId) — validated in TimetableSwapService
 *  since which one applies depends on the session being swapped, not the request alone. */
public record SwapRequest(

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    Long periodId,

    Long labSlotId
) {}
