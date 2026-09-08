package com.cms.inventory.approval.dto;

public record ApprovalWorkflowStepResponse(
    Long id,
    Integer stepOrder,
    String stepName,
    Long permissionId,
    String permissionCode,
    String permissionDisplayName
) {}
