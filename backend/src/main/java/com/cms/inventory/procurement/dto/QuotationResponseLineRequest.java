package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for recording (or updating, while the line is still PENDING) one supplier's quote. */
public record QuotationResponseLineRequest(

    @NotNull(message = "Quoted unit price is required")
    @DecimalMin(value = "0", message = "Quoted unit price must not be negative")
    BigDecimal quotedUnitPrice,

    @Min(value = 0, message = "Lead time must not be negative")
    Integer quotedLeadTimeDays,

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
