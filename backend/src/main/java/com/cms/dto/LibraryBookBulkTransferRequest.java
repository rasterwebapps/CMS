package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record LibraryBookBulkTransferRequest(

    @NotEmpty(message = "At least one book must be selected")
    List<Long> bookIds,

    @NotNull(message = "Target shelf is required")
    Long newShelfId,

    String notes
) {}
