package com.cms.inventory.consignment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConsignmentStockReceiveRequest(
    @NotNull Long agreementId,
    @NotNull Long productId,
    @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
    @NotNull @DecimalMin(value = "0.0") BigDecimal consignmentPrice,
    @Size(max = 500) String notes
) {}
