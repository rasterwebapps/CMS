package com.cms.inventory.stock.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code unallocatedQty} is {@code qtyOnHand} minus the sum of {@code allocations} — a balance
 * only ever gets an allocation row once some movement's caller supplied a bin (Goods Receipt,
 * Stock Transfer, Record Stock Movement, or a bin-scoped Cycle Count), so it's normal for this to
 * be nonzero on a balance that predates bins, or one moved entirely through a bin-blind path.
 */
public record StockBalanceBinBreakdownResponse(
    Long balanceId,
    BigDecimal qtyOnHand,
    BigDecimal unallocatedQty,
    List<StockBinAllocationResponse> allocations
) {}
