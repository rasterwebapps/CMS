package com.cms.dto;

import java.math.BigDecimal;

public record SemesterPaymentDetail(
    String semesterLabel,
    Integer yearNumber,
    Integer semesterSequence,
    BigDecimal amountApplied
) {}
