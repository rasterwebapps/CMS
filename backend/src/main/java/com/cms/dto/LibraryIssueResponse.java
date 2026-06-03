package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.IssueStatus;
import com.cms.model.enums.LibraryMemberType;

public record LibraryIssueResponse(
    Long id,
    Long bookId,
    String accessionNumber,
    String bookTitle,
    String bookAuthors,
    String callNumber,
    String shelfLocation,
    LibraryMemberType memberType,
    Long studentId,
    String studentName,
    String studentRollNumber,
    Long facultyId,
    String facultyName,
    String facultyEmployeeCode,
    LocalDate issuedDate,
    LocalDate dueDate,
    LocalDate returnedDate,
    int renewalCount,
    LocalDate lastRenewedDate,
    IssueStatus status,
    String issuedBy,
    String returnedTo,
    String remarks,
    LibraryFineResponse fine,
    Instant createdAt,
    Instant updatedAt
) {}
