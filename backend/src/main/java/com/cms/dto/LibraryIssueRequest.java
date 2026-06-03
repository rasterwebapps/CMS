package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.LibraryMemberType;

import jakarta.validation.constraints.NotNull;

public record LibraryIssueRequest(

    @NotNull(message = "Book ID is required")
    Long bookId,

    @NotNull(message = "Member type is required")
    LibraryMemberType memberType,

    Long studentId,

    Long facultyId,

    LocalDate issuedDate,

    String remarks
) {}
