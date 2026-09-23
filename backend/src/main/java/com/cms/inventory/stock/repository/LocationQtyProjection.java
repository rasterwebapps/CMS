package com.cms.inventory.stock.repository;

import java.math.BigDecimal;

/** One location's total on-hand quantity (summed across every batch/variant) for one product —
 *  the mirror of {@link ProductQtyProjection}, used to find other locations holding surplus of a
 *  product for a Stock Indent's store fulfillment decision. See {@code
 *  StockBalanceRepository.sumQtyByLocationForProduct}. */
public interface LocationQtyProjection {
    Long getLocationId();
    BigDecimal getQty();
}
