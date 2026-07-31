package com.cms.dto;

import java.math.BigDecimal;

public record UnitCoverageDto(
    Long unitId,
    Integer unitNumber,
    String title,
    BigDecimal hoursCovered,
    boolean markedComplete
) {}
