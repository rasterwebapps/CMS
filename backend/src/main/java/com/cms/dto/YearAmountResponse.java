package com.cms.dto;

import java.math.BigDecimal;

public record YearAmountResponse(
    Long id,
    Integer yearNumber,
    String yearLabel,
    BigDecimal amount
) {}
