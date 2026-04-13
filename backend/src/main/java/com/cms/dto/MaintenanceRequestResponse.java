package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.MaintenancePriority;
import com.cms.model.enums.MaintenanceStatus;
import com.cms.model.enums.MaintenanceType;

public record MaintenanceRequestResponse(
    Long id,
    Long equipmentId,
    String equipmentName,
    String equipmentAssetCode,
    Long labId,
    String labName,
    String title,
    String description,
    MaintenanceType maintenanceType,
    MaintenancePriority priority,
    MaintenanceStatus status,
    Long requestedById,
    String requestedByName,
    LocalDate requestDate,
    LocalDate scheduledDate,
    LocalDate completionDate,
    Long assignedToId,
    String assignedToName,
    BigDecimal estimatedCost,
    BigDecimal actualCost,
    String resolutionNotes,
    Instant createdAt,
    Instant updatedAt
) {}
