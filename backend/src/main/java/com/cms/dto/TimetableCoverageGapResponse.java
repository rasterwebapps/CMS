package com.cms.dto;

import java.time.Instant;
import java.util.List;

public record TimetableCoverageGapResponse(
    int status,
    String message,
    List<TimetableCoverageGap> gaps,
    Instant timestamp
) {}
