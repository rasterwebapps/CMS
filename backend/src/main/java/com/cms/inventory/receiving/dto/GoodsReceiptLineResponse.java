package com.cms.inventory.receiving.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoodsReceiptLineResponse(
    Long id,
    Long purchaseOrderItemId,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    BigDecimal orderedQty,
    BigDecimal alreadyReceivedQty,
    BigDecimal receivedQty,
    BigDecimal unitCost,
    String batchOrSerialNo,
    LocalDate expiryDate,
    String notes
) {}
