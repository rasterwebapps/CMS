package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record QuotationRequestAddLineRequest(

    @NotNull(message = "Purchase requisition line is required")
    Long purchaseRequisitionItemId,

    /** Defaults to the requisition line's own requested quantity when omitted. */
    @DecimalMin(value = "0", inclusive = false, message = "Requested quantity must be greater than 0")
    BigDecimal requestedQty
) {}
