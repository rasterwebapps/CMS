package com.cms.inventory.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalActionBypassRequest(
    @NotBlank String reason,
    @Size(max = 500) String notes
) {}
