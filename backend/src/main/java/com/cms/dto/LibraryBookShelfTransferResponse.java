package com.cms.dto;

import java.time.Instant;

public record LibraryBookShelfTransferResponse(
    Long id,
    Long bookId,
    String oldLibraryName,
    String oldRackName,
    String oldShelfName,
    String newLibraryName,
    String newRackName,
    String newShelfName,
    Instant transferredAt,
    String transferredBy,
    String notes
) {}
