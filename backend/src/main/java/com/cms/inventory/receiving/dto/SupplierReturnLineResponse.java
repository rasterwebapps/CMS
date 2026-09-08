package com.cms.inventory.receiving.dto;

import java.math.BigDecimal;

public record SupplierReturnLineResponse(
    Long id,
    Long goodsReceiptLineId,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    BigDecimal receivedQty,
    BigDecimal alreadyReturnedQty,
    BigDecimal returnedQty,
    String notes
) {}
