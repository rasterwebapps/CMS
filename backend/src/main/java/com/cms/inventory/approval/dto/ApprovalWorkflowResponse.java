package com.cms.inventory.approval.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ApprovalWorkflowResponse(
    Long id,
    String name,
    String documentType,
    Long locationId,
    String locationVirtualName,
    BigDecimal minAmount,
    Boolean isActive,
    List<ApprovalWorkflowStepResponse> steps,
    Instant createdAt,
    Instant updatedAt
) {}
