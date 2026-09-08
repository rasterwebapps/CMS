package com.cms.inventory.stock.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockMovementResponse(
    Long ledgerId,
    Long productId,
    Long locationId,
    Long batchId,
    String txnType,
    BigDecimal qtyDelta,
    BigDecimal newQtyOnHand,
    BigDecimal newValueOnHand,
    Instant txnDate
) {}
