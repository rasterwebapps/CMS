package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.LabInChargeRole;

public record LabInChargeAssignmentResponse(
    Long id,
    Long labId,
    String labName,
    Long assigneeId,
    String assigneeName,
    LabInChargeRole role,
    LocalDate assignedDate,
    Instant createdAt,
    Instant updatedAt
) {}
