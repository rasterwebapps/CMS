package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.TermInstanceStatus;

public record TermInstanceUpdateRequest(
    LocalDate startDate,
    LocalDate endDate,
    TermInstanceStatus status
) {}
