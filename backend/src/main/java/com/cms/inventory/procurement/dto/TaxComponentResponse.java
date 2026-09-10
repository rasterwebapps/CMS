package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

/** One snapshotted {@code PurchaseOrderItemTaxComponent} row, as returned on a PO line. */
public record TaxComponentResponse(
    String componentName,
    BigDecimal splitPercentApplied,
    BigDecimal componentAmount
) {}
