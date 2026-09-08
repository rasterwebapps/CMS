package com.cms.inventory.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockMovementRequest(

    @NotNull(message = "Product is required")
    Long productId,

    @NotNull(message = "Location is required")
    Long locationId,

    @Size(max = 100, message = "Batch/serial no. must not exceed 100 characters")
    String batchOrSerialNo,

    LocalDate expiryDate,

    /** RECEIPT, ADJUSTMENT, or DISPOSAL — see the 2026-09-07 "Stock Tracking slice" decision-log entry. */
    @NotBlank(message = "Transaction type is required")
    String txnType,

    /** INCREASE or DECREASE — only meaningful (and required) when txnType is ADJUSTMENT. */
    String direction,

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    BigDecimal quantity,

    BigDecimal unitCost,

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
