package com.cms.inventory.indent.model;

import java.math.BigDecimal;

/** A quantity summed per (product, requesting location) pair — used to net already-open Stock
 *  Indent quantity against a computed reorder shortfall, the same MRP netting role {@code
 *  ProductLocationQtyProjection} plays for the Wanted List. A deliberately separate, minimal
 *  interface rather than reusing that procurement-package one, to keep this module's own
 *  dependency graph from crossing into procurement for a three-field data carrier. See {@code
 *  StockIndentItemRepository.findOpenQtyByProductAndRequestingLocation}. */
public interface IndentOpenQtyProjection {
    Long getProductId();
    Long getLocationId();
    BigDecimal getQty();
}
