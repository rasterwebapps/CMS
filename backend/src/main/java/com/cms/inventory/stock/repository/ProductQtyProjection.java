package com.cms.inventory.stock.repository;

import java.math.BigDecimal;

/** One product's total on-hand quantity (summed across every batch) at a location. */
public interface ProductQtyProjection {
    Long getProductId();
    BigDecimal getQty();
}
