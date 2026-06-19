package com.cms.dto;

import com.cms.model.enums.ProgramStatus;

import jakarta.validation.constraints.NotNull;

public record ProgramStatusUpdateRequest(
    @NotNull(message = "Status is required") ProgramStatus status,
    String reason
) {
}

