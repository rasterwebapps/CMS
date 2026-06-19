package com.cms.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveStatusUpdateRequest(
    @NotNull(message = "isActive is required") Boolean isActive,
    String reason
) {
}

