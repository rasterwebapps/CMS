package com.cms.inventory.receiving.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupplierReturnCreateRequest(
    @NotNull Long goodsReceiptId,
    LocalDate returnDate,
    String reason,
    @Size(max = 500) String notes
) {}
