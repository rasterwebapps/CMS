package com.cms.inventory.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One level of a {@code UomConversionTemplate} being saved. Same shape as {@link
 * ProductUomLevelRequest} — {@code levelRank} 0 must be the template's own base unit ({@code
 * uomId} = the request's {@code baseUomId}, {@code factorToBase} = 1), enforced in {@code
 * UomConversionTemplateService}.
 */
public record UomConversionTemplateLevelRequest(
    @NotNull Long uomId,
    @NotNull @PositiveOrZero Integer levelRank,
    @NotNull @DecimalMin(value = "0.000001", message = "Conversion factor must be greater than zero") BigDecimal factorToBase,
    Boolean isDefaultPurchase
) {}
