package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponse(
    Long id,
    Long supplierId,
    String supplierName,
    Long locationId,
    String locationVirtualName,
    String status,
    LocalDate poDate,
    LocalDate expectedDeliveryDate,
    String currencyCode,
    BigDecimal exchangeRate,
    String notes,
    String createdBy,
    Instant createdAt,
    String orderedBy,
    Instant orderedAt,
    String forceClosedBy,
    Instant forceClosedAt,
    String forceCloseReason,
    int lineCount,
    BigDecimal totalAmount,
    List<PurchaseOrderItemResponse> lines
) {}
