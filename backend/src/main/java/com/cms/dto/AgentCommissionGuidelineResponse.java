package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.LocalityType;

public record AgentCommissionGuidelineResponse(
    Long id,
    Long agentId,
    String agentName,
    Long programId,
    String programName,
    LocalityType localityType,
    BigDecimal suggestedCommission,
    Instant createdAt,
    Instant updatedAt
) {}
