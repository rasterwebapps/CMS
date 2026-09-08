package com.cms.inventory.stock.model;

import java.math.BigDecimal;

/**
 * One (product, location) pair whose summed {@link StockBalance#getQtyOnHand()} currently sits
 * below that product's configured {@code reorderLevel} — the raw candidate set the Wanted List's
 * shortage job nets against open Purchase Requisition quantity before deciding whether a line is
 * actually needed. See {@code StockBalanceRepository.findReorderShortageCandidates}.
 */
public interface ReorderShortageProjection {
    Long getProductId();
    Long getLocationId();
    BigDecimal getQtyOnHand();
    BigDecimal getReorderLevel();
    BigDecimal getReorderQty();
}
