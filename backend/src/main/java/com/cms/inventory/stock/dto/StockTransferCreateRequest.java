package com.cms.inventory.stock.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockTransferCreateRequest(
    @NotNull Long sourceLocationId,
    @NotNull Long destinationLocationId,
    LocalDate transferDate,
    @Size(max = 500) String notes
) {}
