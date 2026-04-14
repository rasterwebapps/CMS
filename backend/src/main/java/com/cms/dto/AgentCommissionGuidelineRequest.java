package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.LocalityType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AgentCommissionGuidelineRequest(
    @NotNull(message = "Agent ID is required")
    Long agentId,

    @NotNull(message = "Program ID is required")
    Long programId,

    @NotNull(message = "Locality type is required")
    LocalityType localityType,

    @NotNull(message = "Suggested commission is required")
    @Positive(message = "Suggested commission must be positive")
    BigDecimal suggestedCommission
) {}
