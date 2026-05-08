package com.cms.dto;

import java.math.BigDecimal;

public record SemesterPaymentDetail(
    String installmentLabel,
    Integer yearNumber,
    Integer sequence,
    BigDecimal amountApplied
) {}
