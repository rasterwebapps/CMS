package com.cms.dto;

import jakarta.validation.constraints.NotNull;

public record CourseStatusUpdateRequest(
    @NotNull(message = "isActive is required") Boolean isActive,
    String reason
) {
}

