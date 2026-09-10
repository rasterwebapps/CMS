package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseOrderItemResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    Long purchaseRequisitionItemId,
    BigDecimal orderedQty,
    Long uomLevelId,
    String enteredUomCode,
    BigDecimal enteredQty,
    BigDecimal unitPrice,
    Long taxRuleId,
    String taxRuleName,
    BigDecimal taxAmount,
    String jurisdictionMode,
    List<TaxComponentResponse> taxComponents,
    BigDecimal lineTotal,
    BigDecimal receivedQty
) {}
