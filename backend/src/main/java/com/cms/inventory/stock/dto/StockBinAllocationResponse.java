package com.cms.inventory.stock.dto;

import java.math.BigDecimal;

public record StockBinAllocationResponse(
    Long binId,
    String binCode,
    String binName,
    String rackName,
    BigDecimal qty
) {}
