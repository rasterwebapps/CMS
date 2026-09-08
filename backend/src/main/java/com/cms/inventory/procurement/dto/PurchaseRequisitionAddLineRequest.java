package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PurchaseRequisitionAddLineRequest(

    @NotNull(message = "Product is required")
    Long productId,

    @NotNull(message = "Requested quantity is required")
    @DecimalMin(value = "0", inclusive = false, message = "Requested quantity must be greater than 0")
    BigDecimal requestedQty,

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
