package com.cms.inventory.catalog.dto;

import java.math.BigDecimal;

public record ProductUomLevelResponse(
    Long id,
    Long uomId,
    String uomCode,
    String uomName,
    Integer levelRank,
    BigDecimal factorToBase,
    Boolean isDefaultPurchase
) {}
