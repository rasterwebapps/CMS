package com.cms.inventory.stock.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CycleCountResponse(
    Long id,
    Long locationId,
    String locationVirtualName,
    String scope,
    String status,
    LocalDate countDate,
    String notes,
    String createdBy,
    Instant createdAt,
    String submittedBy,
    Instant submittedAt,
    Instant completedAt,
    Integer lineCount,
    Integer pendingReviewCount,
    List<CycleCountLineResponse> lines
) {}
