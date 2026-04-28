package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record YearFeeFromEnquiry(
    int yearNumber,
    BigDecimal amount,
    LocalDate dueDate
) {}
