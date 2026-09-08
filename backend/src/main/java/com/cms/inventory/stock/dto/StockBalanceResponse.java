package com.cms.inventory.stock.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StockBalanceResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    Long locationId,
    String locationVirtualName,
    Long batchId,
    String batchOrSerialNo,
    LocalDate expiryDate,
    BigDecimal qtyOnHand,
    BigDecimal valueOnHand,
    Instant lastUpdated
) {}
