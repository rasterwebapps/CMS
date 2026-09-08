package com.cms.inventory.asset.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AssetResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    Long locationId,
    String locationVirtualName,
    String assetTag,
    String serialNumber,
    String status,
    Long goodsReceiptLineId,
    BigDecimal purchaseValue,
    LocalDate purchaseDate,
    Integer usefulLifeMonths,
    BigDecimal salvageValue,
    boolean depreciationApplicable,
    BigDecimal accumulatedDepreciation,
    BigDecimal currentBookValue,
    String disposalReason,
    BigDecimal disposalValue,
    LocalDate disposalDate,
    String disposedBy,
    Instant disposedAt,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {}
