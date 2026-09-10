package com.cms.inventory.receiving.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code uomLevelId} is optional — a level from the receiving product's *active* unit-of-measure
 * chain to receive this line in (e.g. "Carton"), which need not match the unit the PO line was
 * originally ordered in — a single PO line can be received across several lines/receipts in
 * different units (3 Cartons on one line, 2 loose Boxes on another). When set, {@code
 * receivedQty} is read as the quantity *as typed in that unit*, converted to base units via the
 * level's {@code factorToBase} before being persisted/posted to the stock ledger.
 */
public record GoodsReceiptAddLineRequest(
    @NotNull Long purchaseOrderItemId,
    @NotNull @DecimalMin(value = "0.001", message = "Received quantity must be greater than zero") BigDecimal receivedQty,
    Long uomLevelId,
    BigDecimal unitCost,
    @Size(max = 100) String batchOrSerialNo,
    LocalDate expiryDate,
    @Size(max = 500) String notes
) {}
