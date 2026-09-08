package com.cms.inventory.receiving.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SupplierReturnResponse(
    Long id,
    Long goodsReceiptId,
    String supplierName,
    Long locationId,
    String locationVirtualName,
    String status,
    String reason,
    LocalDate returnDate,
    String notes,
    String createdBy,
    Instant createdAt,
    String completedBy,
    Instant completedAt,
    int lineCount,
    List<SupplierReturnLineResponse> lines
) {}
