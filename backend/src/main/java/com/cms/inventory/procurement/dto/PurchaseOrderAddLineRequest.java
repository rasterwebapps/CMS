package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

/**
 * {@code orderedQty}/{@code unitPrice}/{@code taxRuleId} are all optional overrides: {@code
 * orderedQty} defaults to the source requisition line's {@code requestedQty}, {@code unitPrice}
 * defaults to the resolved {@code VendorProductMappingService.EffectiveRate} for (supplier,
 * product) if one exists (required if it doesn't — no silent zero-price line).
 */
public record PurchaseOrderAddLineRequest(
    @NotNull Long purchaseRequisitionItemId,
    BigDecimal orderedQty,
    BigDecimal unitPrice,
    Long taxRuleId
) {}
