package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.LibraryItemType;
import com.cms.model.enums.LibraryMemberType;

import jakarta.validation.constraints.NotNull;

public record LibraryIssueRequest(

    @NotNull(message = "Item type is required")
    LibraryItemType itemType,

    Long bookId,

    Long periodicalId,

    @NotNull(message = "Member type is required")
    LibraryMemberType memberType,

    Long studentId,

    Long facultyId,

    LocalDate issuedDate,

    String remarks
) {}
