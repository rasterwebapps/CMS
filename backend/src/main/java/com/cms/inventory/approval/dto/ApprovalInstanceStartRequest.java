package com.cms.inventory.approval.dto;

import jakarta.validation.constraints.NotNull;

public record ApprovalInstanceStartRequest(
    @NotNull Long workflowId,
    Long purchaseRequisitionId,
    Long purchaseOrderId
) {}
