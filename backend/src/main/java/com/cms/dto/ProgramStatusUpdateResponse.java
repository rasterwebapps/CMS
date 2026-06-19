package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.ProgramStatus;

public record ProgramStatusUpdateResponse(
    Long id,
    ProgramStatus status,
    Instant updatedAt
) {
}

