package com.cms.inventory.receiving.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GoodsReceiptResponse(
    Long id,
    String receiptNumber,
    Long purchaseOrderId,
    String supplierName,
    Long locationId,
    String locationVirtualName,
    String status,
    LocalDate receiptDate,
    String notes,
    String createdBy,
    Instant createdAt,
    String confirmedBy,
    Instant confirmedAt,
    int lineCount,
    List<GoodsReceiptLineResponse> lines
) {}
