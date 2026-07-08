package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.BookSourceOfSupply;
import com.cms.model.enums.BookStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LibraryBookRequest(

    @Size(max = 30, message = "Accession number must not exceed 30 characters")
    String accessionNumber,

    LocalDate entryDate,

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    String title,

    @NotBlank(message = "Author(s) is required")
    @Size(max = 500, message = "Authors must not exceed 500 characters")
    String authors,

    @Size(max = 300, message = "Publisher must not exceed 300 characters")
    String publisher,

    @Size(max = 20, message = "Year of publication must not exceed 20 characters")
    String yearOfPublication,

    @Size(max = 100, message = "Edition must not exceed 100 characters")
    String edition,

    @Size(max = 30, message = "ISBN must not exceed 30 characters")
    String isbn,

    @Size(max = 200, message = "Collation must not exceed 200 characters")
    String collation,

    @Size(max = 200, message = "Series must not exceed 200 characters")
    String series,

    @Size(max = 50, message = "Call number must not exceed 50 characters")
    String callNumber,

    @NotNull(message = "Library is required")
    Long libraryId,

    Long shelfId,

    @Size(max = 100, message = "Subject category must not exceed 100 characters")
    String subjectCategory,

    BookSourceOfSupply sourceOfSupply,

    @Size(max = 200, message = "Vendor/donor name must not exceed 200 characters")
    String vendorDonorName,

    @Size(max = 50, message = "Bill number must not exceed 50 characters")
    String billNumber,

    LocalDate billDate,

    BigDecimal priceRs,

    BookStatus status,

    String remarks
) {}
