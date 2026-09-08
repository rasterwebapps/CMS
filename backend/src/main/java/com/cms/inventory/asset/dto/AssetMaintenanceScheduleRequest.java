package com.cms.inventory.asset.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetMaintenanceScheduleRequest(
    @NotNull Long assetId,
    @NotBlank String scheduleType,
    Integer recurrenceIntervalDays,
    @NotNull LocalDate nextDueDate,
    @Size(max = 500) String notes
) {}
