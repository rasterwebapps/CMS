package com.cms.inventory.consignment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConsignmentStockConsumeRequest(
    @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
    @Size(max = 500) String notes
) {}
