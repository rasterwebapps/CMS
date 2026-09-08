package com.cms.inventory.approval.dto;

import java.time.Instant;

public record ApprovalActionResponse(
    Long id,
    Integer stepOrder,
    String stepName,
    String requiredPermissionCode,
    String requiredPermissionDisplayName,
    String status,
    String actedBy,
    Instant actedAt,
    String notes,
    String exceptionReason,
    /** True when this action is at the instance's current stage, still PENDING, AND the requesting user holds the required permission. */
    boolean actionableByCurrentUser,
    /** True when this action is at the instance's current stage, still PENDING, AND the requesting user holds INVENTORY_APPROVAL_BYPASS. */
    boolean bypassableByCurrentUser
) {}
