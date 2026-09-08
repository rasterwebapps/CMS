package com.cms.inventory.approval.dto;

import java.time.Instant;
import java.util.List;

public record ApprovalInstanceResponse(
    Long id,
    Long workflowId,
    String workflowName,
    String documentType,
    Long purchaseRequisitionId,
    Long purchaseOrderId,
    String status,
    Integer currentStepOrder,
    String initiatedBy,
    Instant initiatedAt,
    Instant completedAt,
    List<ApprovalActionResponse> actions
) {}
