package com.cms.inventory.procurement.model;

import java.math.BigDecimal;

/** A quantity summed per (product, location) pair — used to net already-open Purchase
 *  Requisition quantity against a computed reorder shortfall. See
 *  {@code PurchaseRequisitionItemRepository.findOpenQtyByProductAndLocation}. */
public interface ProductLocationQtyProjection {
    Long getProductId();
    Long getLocationId();
    BigDecimal getQty();
}
