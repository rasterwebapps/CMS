package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

public record RateContractLineResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    BigDecimal negotiatedRate
) {}
