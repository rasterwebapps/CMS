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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.LibraryIssueResponse;
import com.cms.model.enums.LibraryMemberType;
import com.cms.service.LibraryIssueExportService;
import com.cms.service.LibraryIssueService;
import com.cms.util.ExportSortUtils;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.ExportResponseFactory;

@RestController
@RequestMapping("/library/reports")
public class LibraryReportController {

    private static final Map<String, String> OVERDUE_SORT_FIELDS = new LinkedHashMap<>();
    static {
        OVERDUE_SORT_FIELDS.put("dueDate", "Due Date");
        OVERDUE_SORT_FIELDS.put("issuedDate", "Issued Date");
        OVERDUE_SORT_FIELDS.put("status", "Status");
        OVERDUE_SORT_FIELDS.put("memberType", "Member Type");
    }

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

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, OVERDUE_SORT_FIELDS.keySet(), "dueDate", Sort.Direction.ASC);
        List<LibraryIssueResponse> data = issueService.findAllOverdueMatching(search, memberType, exportSort);

        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "dueDate", Sort.Direction.ASC);
        ExportMetadata meta = ExportMetadata.of("Overdue Books Export")
            .filter("Search", search)
            .filter("Member Type", memberType != null ? memberType.name() : null)
            .sort(OVERDUE_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "overdue-books",
            () -> issueExportService.toExcel(data, meta),
            () -> issueExportService.toPdf(data, meta));
    }
}
