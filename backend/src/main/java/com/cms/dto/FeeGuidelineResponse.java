package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

public record FeeGuidelineResponse(
    BigDecimal totalFee,
    List<FeeStructureResponse> items
) {}
