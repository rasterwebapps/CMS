package com.cms.inventory.receiving.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GoodsReceiptAddLineRequest(
    @NotNull Long purchaseOrderItemId,
    @NotNull @DecimalMin(value = "0.001", message = "Received quantity must be greater than zero") BigDecimal receivedQty,
    BigDecimal unitCost,
    @Size(max = 100) String batchOrSerialNo,
    LocalDate expiryDate,
    @Size(max = 500) String notes
) {}
