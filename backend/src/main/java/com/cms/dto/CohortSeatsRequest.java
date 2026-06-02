package com.cms.dto;

import java.math.BigDecimal;

public record CohortSeatsRequest(
    Integer    totalSeats,
    BigDecimal managementPercentage
) {}
