package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RateContractLineRequest(

    @NotNull(message = "Product is required")
    Long productId,

    @NotNull(message = "Negotiated rate is required")
    @DecimalMin(value = "0", message = "Negotiated rate cannot be negative")
    BigDecimal negotiatedRate
) {}
