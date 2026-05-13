package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record ProgramTransferRequest(
    @NotNull(message = "New program ID is required")
    Long newProgramId,

    List<Long> documentIdsToReturn,

    boolean consentConfirmed,

    String notes
) {}
