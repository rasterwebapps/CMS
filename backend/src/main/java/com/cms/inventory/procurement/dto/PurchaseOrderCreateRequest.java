package com.cms.inventory.procurement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record PurchaseOrderCreateRequest(
    @NotNull Long supplierId,
    @NotNull Long locationId,
    LocalDate poDate,
    LocalDate expectedDeliveryDate,
    String currencyCode,
    java.math.BigDecimal exchangeRate,
    String notes
) {}
