package com.cms.inventory.asset.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AssetMaintenanceScheduleResponse(
    Long id,
    Long assetId,
    String assetTag,
    String productName,
    String scheduleType,
    Integer recurrenceIntervalDays,
    LocalDate nextDueDate,
    LocalDate lastPerformedDate,
    boolean overdue,
    Boolean isActive,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {}
