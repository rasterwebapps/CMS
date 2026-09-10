package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

/**
 * {@code orderedQty}/{@code unitPrice}/{@code taxRuleId} are all optional overrides: {@code
 * orderedQty} defaults to the source requisition line's {@code requestedQty} (always base-unit),
 * {@code unitPrice} defaults to the resolved {@code VendorProductMappingService.EffectiveRate}
 * for (supplier, product) if one exists (required if it doesn't — no silent zero-price line).
 * {@code uomLevelId} is optional — a level from the product's *active* unit-of-measure chain
 * (see {@code ProductUomChainService}) to order in instead of the base unit. When set, {@code
 * orderedQty} is read as the quantity *as typed in that unit* (e.g. "5" Cartons), converted to
 * base units via the level's {@code factorToBase} before being persisted; {@code unitPrice} is
 * still the price for one of that unit (a carton's price, not one base-unit item's).
 */
public record PurchaseOrderAddLineRequest(
    @NotNull Long purchaseRequisitionItemId,
    BigDecimal orderedQty,
    Long uomLevelId,
    BigDecimal unitPrice,
    Long taxRuleId
) {}
