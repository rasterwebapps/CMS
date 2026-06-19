package com.cms.dto;

import java.time.Instant;

public record LifecycleConflictResponse(
    int status,
    String message,
    String code,
    String entity,
    Long entityId,
    Integer blockerCount,
    Instant timestamp
) {
}

