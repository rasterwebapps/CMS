package com.cms.inventory.issue.dto;

import jakarta.validation.constraints.Size;

public record LoanableItemIssueReturnRequest(
    @Size(max = 500) String conditionOnReturn,
    @Size(max = 500) String notes
) {}
