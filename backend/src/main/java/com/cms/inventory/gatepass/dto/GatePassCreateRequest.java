package com.cms.inventory.gatepass.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GatePassCreateRequest(
    @NotNull String direction,
    @NotNull Boolean returnable,
    Long productId,
    Long assetId,
    @NotNull Long locationId,
    @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
    @NotBlank @Size(max = 500) String reason,
    @NotBlank @Size(max = 200) String partyName,
    @Size(max = 100) String partyContact,
    Long linkedPurchaseOrderId,
    LocalDate passDate,
    LocalDate expectedReturnDate,
    @Size(max = 500) String notes
) {}
