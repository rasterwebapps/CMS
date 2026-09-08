package com.cms.inventory.stock.model;

import java.math.BigDecimal;

/**
 * One category's rollup for the Stock Valuation Report — a distinct-product count (not a summed
 * quantity: products in the same category can carry different UOMs, so summing raw {@code
 * qtyOnHand} across them would be meaningless) plus total on-hand value (always safely summable,
 * money being money regardless of a product's own UOM). See {@code
 * StockBalanceRepository.sumValuationByCategory} and the "Stock Valuation Report slice"
 * decision-log entry.
 */
public interface StockValuationByCategoryProjection {
    Long getCategoryId();
    String getCategoryName();
    Long getProductCount();
    BigDecimal getTotalValue();
}
