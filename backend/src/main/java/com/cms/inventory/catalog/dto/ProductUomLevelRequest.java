package com.cms.inventory.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One level of a new (or reactivated) {@code ProductUomChainVersion}. {@code levelRank} 0 must be
 * the product's base unit ({@code uomId} = the product's {@code baseUomId}, {@code factorToBase}
 * = 1) — enforced in {@code ProductUomChainService}, not by validation here, since it depends on
 * the product being saved against.
 */
public record ProductUomLevelRequest(
    @NotNull Long uomId,
    @NotNull @PositiveOrZero Integer levelRank,
    @NotNull @DecimalMin(value = "0.000001", message = "Conversion factor must be greater than zero") BigDecimal factorToBase,
    Boolean isDefaultPurchase
) {}
