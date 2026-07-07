package com.cms.dto;

import com.cms.model.enums.BookStatus;
import com.cms.model.enums.LibraryItemType;

/** Resolves an accession number to either a book or a journal for the Issue Book screen. */
public record LibraryCirculationLookupResponse(
    LibraryItemType itemType,
    Long itemId,
    String accessionNumber,
    String title,
    String detail,
    String callNumber,
    String shelfLocation,
    BookStatus status
) {}
