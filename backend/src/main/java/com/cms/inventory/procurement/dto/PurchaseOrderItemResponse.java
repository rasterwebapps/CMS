package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    Long purchaseRequisitionItemId,
    BigDecimal orderedQty,
    BigDecimal unitPrice,
    Long taxRuleId,
    String taxRuleName,
    BigDecimal taxAmount,
    BigDecimal lineTotal,
    BigDecimal receivedQty
) {}
