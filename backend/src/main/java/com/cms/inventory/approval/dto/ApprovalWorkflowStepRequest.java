package com.cms.inventory.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApprovalWorkflowStepRequest(
    @NotNull Integer stepOrder,
    @NotBlank String stepName,
    @NotBlank String permissionCode
) {}
