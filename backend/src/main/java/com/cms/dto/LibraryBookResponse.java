package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.BookSourceOfSupply;
import com.cms.model.enums.BookStatus;

public record LibraryBookResponse(
    Long id,
    String accessionNumber,
    LocalDate entryDate,
    String title,
    String authors,
    String publisher,
    String yearOfPublication,
    String edition,
    String isbn,
    String collation,
    String series,
    String callNumber,
    String shelfLocation,
    String subjectCategory,
    BookSourceOfSupply sourceOfSupply,
    String vendorDonorName,
    String billNumber,
    LocalDate billDate,
    BigDecimal priceRs,
    BookStatus status,
    String remarks,
    Instant createdAt,
    Instant updatedAt
) {}
