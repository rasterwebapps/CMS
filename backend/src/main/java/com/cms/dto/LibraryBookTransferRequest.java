package com.cms.dto;

import jakarta.validation.constraints.NotNull;

/** The target shelf tier fully determines the rack and library (shelf -> rack -> library). */
public record LibraryBookTransferRequest(

    @NotNull(message = "Target shelf is required")
    Long newShelfId,

    String notes
) {}
