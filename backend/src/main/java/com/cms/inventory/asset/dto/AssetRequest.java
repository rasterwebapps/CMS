package com.cms.inventory.asset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetRequest(
    @NotNull Long productId,
    @NotNull Long locationId,
    @NotBlank @Size(max = 50) String assetTag,
    @Size(max = 100) String serialNumber,
    Long goodsReceiptLineId,
    BigDecimal purchaseValue,
    LocalDate purchaseDate,
    Integer usefulLifeMonths,
    BigDecimal salvageValue,
    @Size(max = 500) String notes
) {}
