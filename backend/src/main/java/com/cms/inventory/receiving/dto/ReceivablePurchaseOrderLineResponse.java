package com.cms.inventory.receiving.dto;

import java.math.BigDecimal;

/** A PO line still open to receive against (orderedQty > receivedQty) — the GRN line picker's pool. */
public record ReceivablePurchaseOrderLineResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    BigDecimal orderedQty,
    BigDecimal receivedQty,
    BigDecimal openQty,
    BigDecimal unitPrice
) {}
