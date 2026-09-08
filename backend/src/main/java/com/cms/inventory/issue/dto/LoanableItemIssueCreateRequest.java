package com.cms.inventory.issue.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoanableItemIssueCreateRequest(
    @NotNull Long productId,
    @NotNull Long locationId,
    @NotBlank @Size(max = 200) String borrowerName,
    @Size(max = 100) String borrowerContact,
    LocalDate issueDate,
    @NotNull LocalDate expectedReturnDate,
    @Size(max = 500) String conditionOnIssue,
    @Size(max = 500) String notes
) {}
