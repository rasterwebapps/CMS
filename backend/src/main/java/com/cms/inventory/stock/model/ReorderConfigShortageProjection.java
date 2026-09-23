package com.cms.inventory.stock.model;

import java.math.BigDecimal;

/**
 * One (product, location) pair whose summed {@link StockBalance#getQtyOnHand()} currently sits
 * below its {@link ProductLocationReorderConfig}'s reorder level — the raw candidate set {@code
 * AutoIndentService} nets against open Stock Indent quantity before deciding whether an
 * auto-generated indent is actually needed. Deliberately a separate projection from {@link
 * ReorderShortageProjection} (the Wanted List's own, keyed off {@code Product.reorderLevel}) even
 * though the shape is similar — this one is scoped to configured (product, location) pairs, not
 * every active product. See {@code ProductLocationReorderConfigRepository.findShortageCandidates}.
 */
public interface ReorderConfigShortageProjection {
    Long getConfigId();
    Long getProductId();
    Long getLocationId();
    BigDecimal getQtyOnHand();
    BigDecimal getReorderLevel();
    BigDecimal getReorderQty();
    BigDecimal getMaxStockQty();
    Long getDefaultSupplyingLocationId();
}
