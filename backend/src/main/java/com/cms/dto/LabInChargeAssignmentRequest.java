package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.LabInChargeRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LabInChargeAssignmentRequest(
    @NotNull(message = "Assignee ID is required")
    Long assigneeId,

    @NotBlank(message = "Assignee name is required")
    @Size(max = 255, message = "Assignee name must not exceed 255 characters")
    String assigneeName,

    @NotNull(message = "Role is required")
    LabInChargeRole role,

    @NotNull(message = "Assigned date is required")
    LocalDate assignedDate
) {}
