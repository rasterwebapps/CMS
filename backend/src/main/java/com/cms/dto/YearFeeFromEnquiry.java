package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record YearFeeFromEnquiry(
    Integer yearNumber,
    BigDecimal amount,
    LocalDate suggestedDueDate
) {}
