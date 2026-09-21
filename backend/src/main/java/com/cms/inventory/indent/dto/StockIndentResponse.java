package com.cms.inventory.indent.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StockIndentResponse(
    Long id,
    Long requestingLocationId,
    String requestingLocationVirtualName,
    Long issuingLocationId,
    String issuingLocationVirtualName,
    String status,
    LocalDate requestDate,
    String notes,
    String createdBy,
    Instant createdAt,
    String submittedBy,
    Instant submittedAt,
    Instant completedAt,
    int lineCount,
    int pendingCount,
    List<StockIndentItemResponse> lines
) {}
