package com.cms.inventory.catalog.dto;

import java.math.BigDecimal;

public record UomConversionTemplateLevelResponse(
    Long uomId,
    String uomCode,
    String uomName,
    Integer levelRank,
    BigDecimal factorToBase,
    Boolean isDefaultPurchase
) {}
