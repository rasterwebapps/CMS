package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.BlockType;
import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BlockedPeriodRequest(
    @NotNull(message = "Period ID is required")
    Long periodId,

    @NotNull(message = "Block type is required")
    BlockType blockType,

    /** Required when blockType == ONE_OFF; ignored otherwise. */
    LocalDate specificDate,

    /** Required when blockType == RECURRING; ignored otherwise. */
    DayOfWeek dayOfWeek,

    /** Required when blockType == RECURRING; ignored otherwise. */
    LocalDate rangeStartDate,

    /** Required when blockType == RECURRING; ignored otherwise. */
    LocalDate rangeEndDate,

    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    String reason
) {}