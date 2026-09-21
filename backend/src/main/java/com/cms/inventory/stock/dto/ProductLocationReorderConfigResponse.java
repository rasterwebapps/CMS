package com.cms.inventory.stock.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductLocationReorderConfigResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    Long locationId,
    String locationVirtualName,
    Long defaultSupplyingLocationId,
    String defaultSupplyingLocationVirtualName,
    BigDecimal reorderLevel,
    BigDecimal reorderQty,
    BigDecimal maxStockQty,
    Boolean autoIndentEnabled,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
