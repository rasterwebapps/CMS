package com.cms.dto;

import java.math.BigDecimal;

import com.cms.model.enums.BookStatus;

public record LibraryReturnRequest(
    String remarks,
    BookStatus resolutionStatus,
    BigDecimal penaltyFine
) {
    public LibraryReturnRequest(String remarks) {
        this(remarks, null, null);
    }
}
