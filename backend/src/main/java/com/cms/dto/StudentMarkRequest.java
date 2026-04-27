package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.MarkStatus;

import jakarta.validation.constraints.NotNull;

public record StudentMarkRequest(
    @NotNull Long examEventId,
    @NotNull Long courseRegistrationId,
    @NotNull MarkStatus markStatus,
    BigDecimal marksObtained,
    String remarks
) {}
