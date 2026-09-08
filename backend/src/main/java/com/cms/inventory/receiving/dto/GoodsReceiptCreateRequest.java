package com.cms.inventory.receiving.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GoodsReceiptCreateRequest(
    @NotNull Long purchaseOrderId,
    LocalDate receiptDate,
    @Size(max = 500) String notes
) {}
