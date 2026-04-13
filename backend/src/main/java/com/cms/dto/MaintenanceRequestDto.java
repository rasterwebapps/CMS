package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.MaintenancePriority;
import com.cms.model.enums.MaintenanceStatus;
import com.cms.model.enums.MaintenanceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaintenanceRequestDto(
    @NotNull(message = "Equipment ID is required")
    Long equipmentId,

    @NotBlank(message = "Title is required")
    String title,

    String description,

    @NotNull(message = "Maintenance type is required")
    MaintenanceType maintenanceType,

    @NotNull(message = "Priority is required")
    MaintenancePriority priority,

    @NotNull(message = "Status is required")
    MaintenanceStatus status,

    Long requestedById,

    @NotNull(message = "Request date is required")
    LocalDate requestDate,

    LocalDate scheduledDate,

    LocalDate completionDate,

    Long assignedToId,

    BigDecimal estimatedCost,

    BigDecimal actualCost,

    String resolutionNotes
) {}
