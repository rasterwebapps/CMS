package com.cms.inventory.receiving.dto;

import java.math.BigDecimal;

/** A confirmed receipt line still holding a returnable quantity — the add-line picker's pool. */
public record ReturnableGoodsReceiptLineResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    BigDecimal receivedQty,
    BigDecimal alreadyReturnedQty,
    BigDecimal openQty
) {}
