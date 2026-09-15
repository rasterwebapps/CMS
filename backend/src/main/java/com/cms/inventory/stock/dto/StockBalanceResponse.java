package com.cms.inventory.stock.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StockBalanceResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    Long variantId,
    String variantCode,
    String variantName,
    /** True once the product has any active {@code ProductVariant} — lets the UI flag a
     *  {@code variantId == null} row as a stranded/unassigned balance needing conversion,
     *  rather than a normal non-variant product's balance. */
    boolean productHasActiveVariants,
    Long locationId,
    String locationVirtualName,
    Long batchId,
    String batchOrSerialNo,
    LocalDate expiryDate,
    BigDecimal qtyOnHand,
    BigDecimal valueOnHand,
    Instant lastUpdated
) {}
