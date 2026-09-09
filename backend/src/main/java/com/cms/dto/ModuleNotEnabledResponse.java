package com.cms.dto;

import java.time.Instant;

public record ModuleNotEnabledResponse(
    int status,
    String message,
    String code,
    String moduleCode,
    Instant timestamp
) {
}
