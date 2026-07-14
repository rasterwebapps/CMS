package com.cms.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LibraryCirculationLookupResponse;
import com.cms.dto.LibraryIssueRequest;
import com.cms.dto.LibraryIssueResponse;
import com.cms.dto.LibraryReturnRequest;
import com.cms.dto.LibraryRenewRequest;
import com.cms.model.enums.IssueStatus;
import com.cms.model.enums.LibraryItemType;
import com.cms.model.enums.LibraryMemberType;
import com.cms.service.LibraryIssueExportService;
import com.cms.service.LibraryIssueService;
import com.cms.util.ExportSortUtils;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.ExportResponseFactory;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/library/issues")
public class LibraryIssueController {

    private static final Map<String, String> EXPORT_SORT_FIELDS = new LinkedHashMap<>();
    static {
        EXPORT_SORT_FIELDS.put("issuedDate", "Issued Date");
        EXPORT_SORT_FIELDS.put("dueDate", "Due Date");
        EXPORT_SORT_FIELDS.put("status", "Status");
    }

    private final LibraryIssueService issueService;
    private final LibraryIssueExportService issueExportService;

    public LibraryIssueController(LibraryIssueService issueService, LibraryIssueExportService issueExportService) {
        this.issueService = issueService;
        this.issueExportService = issueExportService;
    }

    @PostMapping
    @PreAuthorize("@perm.hasAny('LIBRARY_ISSUE_MANAGE', 'LIBRARY_QUICK_ISSUE')")
    public ResponseEntity<LibraryIssueResponse> issue(
            @Valid @RequestBody LibraryIssueRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String issuedBy = jwt != null ? jwt.getClaimAsString("preferred_username") : "librarian";
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.issue(request, issuedBy));
    }

    @GetMapping("/lookup")
    @PreAuthorize("@perm.hasAny('LIBRARY_ISSUE_MANAGE', 'LIBRARY_QUICK_ISSUE')")
    public ResponseEntity<LibraryCirculationLookupResponse> lookup(@RequestParam String accessionNumber) {
        return ResponseEntity.ok(issueService.lookupByAccessionNumber(accessionNumber));
    }

    /** Scan-to-return: resolve a scanned/typed accession number or barcode to its active issue. */
    @GetMapping("/lookup-active")
    @PreAuthorize("@perm.has('LIBRARY_ISSUE_MANAGE')")
    public ResponseEntity<LibraryIssueResponse> lookupActive(@RequestParam String code) {
        return ResponseEntity.ok(issueService.lookupActiveIssueByCode(code));
    }

    @GetMapping
    @PreAuthorize("@perm.has('LIBRARY_ISSUE_MANAGE')")
    public ResponseEntity<List<LibraryIssueResponse>> findAll(
            @RequestParam(required = false) LibraryMemberType memberType,
            @RequestParam(required = false) IssueStatus status) {
        return ResponseEntity.ok(issueService.findAll(memberType, status));
    }

    /** Current authenticated user's own issues (student/faculty portal). */
    @GetMapping("/my")
    @PreAuthorize("@perm.hasAny('LIBRARY_ISSUE_VIEW', 'LIBRARY_ISSUE_MANAGE', 'MY_LIBRARY_VIEW')")
    public ResponseEntity<List<LibraryIssueResponse>> myIssues(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "";
        return ResponseEntity.ok(issueService.findMyIssues(username));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.hasAny('LIBRARY_ISSUE_VIEW', 'LIBRARY_ISSUE_MANAGE', 'MY_LIBRARY_VIEW')")
    public ResponseEntity<LibraryIssueResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.findById(id));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("@perm.has('LIBRARY_ISSUE_MANAGE')")
    public ResponseEntity<List<LibraryIssueResponse>> findByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(issueService.findByStudentId(studentId));
    }

    @GetMapping("/faculty/{facultyId}")
    @PreAuthorize("@perm.has('LIBRARY_ISSUE_MANAGE')")
    public ResponseEntity<List<LibraryIssueResponse>> findByFaculty(@PathVariable Long facultyId) {
        return ResponseEntity.ok(issueService.findByFacultyId(facultyId));
    }

    /** Full circulation history for one book — backs the "View History" action on the Book Catalogue. */
    @GetMapping("/book/{bookId}")
    @PreAuthorize("@perm.has('LIBRARY_CATALOGUE_VIEW_HISTORY')")
    public ResponseEntity<List<LibraryIssueResponse>> findByBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(issueService.findByBookId(bookId));
    }

    /** Full circulation history for one periodical — backs the "View History" action on Journals. */
    @GetMapping("/periodical/{periodicalId}")
    @PreAuthorize("@perm.has('LIBRARY_PERIODICAL_VIEW_HISTORY')")
    public ResponseEntity<List<LibraryIssueResponse>> findByPeriodical(@PathVariable Long periodicalId) {
        return ResponseEntity.ok(issueService.findByPeriodicalId(periodicalId));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("@perm.has('LIBRARY_ISSUE_MANAGE')")
    public ResponseEntity<LibraryIssueResponse> returnBook(
            @PathVariable Long id,
            @RequestBody(required = false) LibraryReturnRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String returnedTo = jwt != null ? jwt.getClaimAsString("preferred_username") : "librarian";
        return ResponseEntity.ok(issueService.returnBook(id, request, returnedTo));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("@perm.has('LIBRARY_ISSUE_MANAGE')")
    public ResponseEntity<LibraryIssueResponse> renew(
            @PathVariable Long id,
            @RequestBody(required = false) LibraryRenewRequest request) {
        return ResponseEntity.ok(issueService.renew(id, request));
    }

    @GetMapping("/page")
    @PreAuthorize("@perm.has('LIBRARY_ISSUE_MANAGE')")
    public ResponseEntity<Page<LibraryIssueResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) LibraryMemberType memberType,
            @RequestParam(required = false) LibraryItemType itemType,
            @PageableDefault(size = 25, sort = "issuedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(issueService.findPage(search, status, memberType, itemType, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('LIBRARY_ISSUE_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) LibraryMemberType memberType,
            @RequestParam(required = false) LibraryItemType itemType,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, EXPORT_SORT_FIELDS.keySet(), "issuedDate", Sort.Direction.DESC);
        List<LibraryIssueResponse> data = issueService.findAllMatching(
            search, status, memberType, itemType, exportSort);

        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "issuedDate", Sort.Direction.DESC);
        ExportMetadata meta = ExportMetadata.of("Issue Register Export")
            .filter("Search", search)
            .filter("Status", status != null ? status.name() : null)
            .filter("Member Type", memberType != null ? memberType.name() : null)
            .filter("Item Type", itemType != null ? itemType.name() : null)
            .sort(EXPORT_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "issue-register",
            () -> issueExportService.toExcel(data, meta),
            () -> issueExportService.toPdf(data, meta));
    }
}
