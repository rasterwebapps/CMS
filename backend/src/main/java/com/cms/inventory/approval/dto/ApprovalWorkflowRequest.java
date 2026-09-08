package com.cms.inventory.approval.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ApprovalWorkflowRequest(
    @NotBlank String name,
    @NotBlank String documentType,
    Long locationId,
    BigDecimal minAmount,
    Boolean isActive,
    @NotNull @NotEmpty @Valid List<ApprovalWorkflowStepRequest> steps
) {}
