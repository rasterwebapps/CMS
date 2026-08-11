package com.cms.dto;

import java.time.Instant;
import java.util.List;

public record TimetableConstraintViolationResponse(
    int status,
    String message,
    List<ConstraintViolation> violations,
    Instant timestamp
) {
}
