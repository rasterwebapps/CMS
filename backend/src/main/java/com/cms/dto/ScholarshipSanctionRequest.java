package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PUT /scholarship-applications/{id}/sanction}.
 *
 * <p>Used after the college has approved an application and the government portal
 * (NSP, ePass TN) has issued an official sanction number. The status moves
 * APPROVED → SANCTIONED, recording who confirmed the sanction and when.</p>
 */
public record ScholarshipSanctionRequest(
    @NotBlank(message = "Govt sanction number is required")
    @Size(max = 50, message = "Sanction number must not exceed 50 characters")
    String govtSanctionNumber,

    @NotNull(message = "Sanction date is required")
    @PastOrPresent(message = "Sanction date cannot be in the future")
    LocalDate sanctionDate,

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    String remarks
) {}

