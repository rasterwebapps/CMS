package com.cms.inventory.asset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssetDisposalRequest(
    LocalDate disposalDate,
    BigDecimal disposalValue,
    @NotBlank @Size(max = 500) String reason
) {}
