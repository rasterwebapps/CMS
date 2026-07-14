package com.cms.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LibraryIssueResponse;
import com.cms.model.enums.LibraryMemberType;
import com.cms.service.LibraryIssueExportService;
import com.cms.service.LibraryIssueService;
import com.cms.util.ExportSortUtils;

@RestController
@RequestMapping("/library/reports")
public class LibraryReportController {

    private static final Set<String> OVERDUE_SORT_FIELDS = Set.of("dueDate", "issuedDate", "status", "memberType");

    private final LibraryIssueService issueService;
    private final LibraryIssueExportService issueExportService;

    public LibraryReportController(LibraryIssueService issueService,
                                    LibraryIssueExportService issueExportService) {
        this.issueService = issueService;
        this.issueExportService = issueExportService;
    }

    /** Effectively-overdue issues, paginated/filtered/sorted. */
    @GetMapping("/overdue/page")
    @PreAuthorize("@perm.has('LIBRARY_REPORT_VIEW')")
    public ResponseEntity<Page<LibraryIssueResponse>> overdueReportPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LibraryMemberType memberType,
            @PageableDefault(size = 25, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(issueService.findOverduePage(search, memberType, pageable));
    }

    @GetMapping("/overdue/export")
    @PreAuthorize("@perm.has('LIBRARY_REPORT_EXPORT')")
    public ResponseEntity<byte[]> overdueReportExport(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LibraryMemberType memberType,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(sort, direction, OVERDUE_SORT_FIELDS, "dueDate", Sort.Direction.ASC);
        List<LibraryIssueResponse> data = issueService.findAllOverdueMatching(search, memberType, exportSort);

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = issueExportService.toPdf(data);
                String filename = "overdue-books-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = issueExportService.toExcel(data);
                String filename = "overdue-books-" + LocalDate.now() + ".xlsx";
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
