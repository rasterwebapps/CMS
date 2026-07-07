package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LibraryBookResponse;
import com.cms.dto.LibraryIssueResponse;
import com.cms.model.enums.BookStatus;
import com.cms.model.enums.IssueStatus;
import com.cms.model.enums.LibraryMemberType;
import com.cms.service.LibraryBookService;
import com.cms.service.LibraryIssueService;

@RestController
@RequestMapping("/library/reports")
@PreAuthorize("@perm.has('LIBRARY_REPORT_VIEW')")
public class LibraryReportController {

    private final LibraryIssueService issueService;
    private final LibraryBookService  bookService;

    public LibraryReportController(LibraryIssueService issueService,
                                    LibraryBookService bookService) {
        this.issueService = issueService;
        this.bookService  = bookService;
    }

    /** All currently overdue issues — used for Overdue Report and fines summary. */
    @GetMapping("/overdue")
    public ResponseEntity<List<LibraryIssueResponse>> overdueReport() {
        return ResponseEntity.ok(issueService.findEffectivelyOverdue());
    }

    /** All issues that have an associated fine (pending, waived, or collected). */
    @GetMapping("/fines")
    public ResponseEntity<List<LibraryIssueResponse>> fineReport(
            @RequestParam(required = false) LibraryMemberType memberType) {
        List<LibraryIssueResponse> all = issueService.findAll(memberType, null);
        return ResponseEntity.ok(all.stream().filter(i -> i.fine() != null).toList());
    }

    /** Full issue history, optionally filtered by member type or date range. */
    @GetMapping("/issue-history")
    public ResponseEntity<List<LibraryIssueResponse>> issueHistoryReport(
            @RequestParam(required = false) LibraryMemberType memberType,
            @RequestParam(required = false) IssueStatus status) {
        return ResponseEntity.ok(issueService.findAll(memberType, status));
    }

    /**
     * Accession Register — complete book catalogue sorted by accession number.
     * Returned as structured data; the frontend handles print formatting.
     */
    @GetMapping("/accession-register")
    public ResponseEntity<List<LibraryBookResponse>> accessionRegister(
            @RequestParam(required = false) BookStatus status,
            @RequestParam(required = false) String subjectCategory) {
        List<LibraryBookResponse> books = status != null
            ? bookService.findByStatus(status)
            : bookService.findAll();
        if (subjectCategory != null && !subjectCategory.isBlank()) {
            books = books.stream()
                .filter(b -> subjectCategory.equalsIgnoreCase(b.subjectCategory()))
                .toList();
        }
        return ResponseEntity.ok(books);
    }
}
