package com.cms.inventory.issue.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StockIssueRequestResponse(
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
    List<StockIssueRequestItemResponse> lines
) {}
