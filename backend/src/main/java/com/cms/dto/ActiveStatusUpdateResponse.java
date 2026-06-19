package com.cms.dto;

import java.time.Instant;

public record ActiveStatusUpdateResponse(
    Long id,
    Boolean isActive,
    Instant updatedAt
) {
}

