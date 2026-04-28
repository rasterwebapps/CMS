package com.cms.dto;

import java.math.BigDecimal;

public record SemesterPaymentDetail(
    String semesterLabel,
    int yearNumber,
    int semesterSequence,
    BigDecimal amountPaid
) {}

