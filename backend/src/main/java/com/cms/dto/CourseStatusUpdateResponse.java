package com.cms.dto;

import java.time.Instant;

public record CourseStatusUpdateResponse(
    Long id,
    Boolean isActive,
    Instant updatedAt
) {
}

