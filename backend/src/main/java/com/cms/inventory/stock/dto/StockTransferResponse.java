package com.cms.inventory.stock.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StockTransferResponse(
    Long id,
    Long sourceLocationId,
    String sourceLocationVirtualName,
    Long destinationLocationId,
    String destinationLocationVirtualName,
    String status,
    LocalDate transferDate,
    String notes,
    String createdBy,
    Instant createdAt,
    String completedBy,
    Instant completedAt,
    int lineCount,
    List<StockTransferLineResponse> lines
) {}
