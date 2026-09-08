package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RateContractRequest(

    @NotNull(message = "Supplier is required")
    Long supplierId,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    LocalDate endDate,

    @DecimalMin(value = "0", message = "Contract value cap cannot be negative")
    BigDecimal contractValueCap,

    @Size(max = 2000, message = "Terms must not exceed 2000 characters")
    String termsText,

    LocalDate renewalReminderDate,

    Boolean isActive
) {}
