package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/library/issues")
public class LibraryIssueController {

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
            @RequestParam(required = false) LibraryItemType itemType) {

        List<LibraryIssueResponse> data = issueService.findAllMatching(
            search, status, memberType, itemType, Sort.by("issuedDate").descending());

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = issueExportService.toPdf(data);
                String filename = "issue-register-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = issueExportService.toExcel(data);
                String filename = "issue-register-" + LocalDate.now() + ".xlsx";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
