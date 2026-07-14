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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.FacultyRequest;
import com.cms.dto.FacultyResponse;
import com.cms.model.Speciality;
import com.cms.model.enums.FacultyStatus;
import com.cms.repository.SpecialityRepository;
import com.cms.service.FacultyExportService;
import com.cms.service.FacultyService;
import com.cms.util.ExportSortUtils;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.ExportResponseFactory;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/faculty")
public class FacultyController {

    private static final Map<String, String> EXPORT_SORT_FIELDS = new LinkedHashMap<>();
    static {
        EXPORT_SORT_FIELDS.put("firstName", "Full Name");
        EXPORT_SORT_FIELDS.put("employeeCode", "Employee Code");
        EXPORT_SORT_FIELDS.put("phone", "Phone");
        EXPORT_SORT_FIELDS.put("email", "Email");
        EXPORT_SORT_FIELDS.put("speciality.name", "Speciality");
        EXPORT_SORT_FIELDS.put("designation.name", "Designation");
        EXPORT_SORT_FIELDS.put("status", "Status");
    }

    private final FacultyService       facultyService;
    private final FacultyExportService facultyExportService;
    private final SpecialityRepository specialityRepository;

    public FacultyController(FacultyService facultyService, FacultyExportService facultyExportService,
                              SpecialityRepository specialityRepository) {
        this.facultyService       = facultyService;
        this.facultyExportService = facultyExportService;
        this.specialityRepository = specialityRepository;
    }

    @PostMapping
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<FacultyResponse> create(@Valid @RequestBody FacultyRequest request) {
        FacultyResponse response = facultyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FacultyResponse>> findAll(
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) FacultyStatus status) {
        List<FacultyResponse> facultyList;
        if (specialityId != null) {
            facultyList = facultyService.findBySpecialityId(specialityId);
        } else if (status != null) {
            facultyList = facultyService.findByStatus(status);
        } else {
            facultyList = facultyService.findAll();
        }
        return ResponseEntity.ok(facultyList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> findById(@PathVariable Long id) {
        FacultyResponse response = facultyService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<FacultyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FacultyRequest request) {
        FacultyResponse response = facultyService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        facultyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/page")
    public ResponseEntity<Page<FacultyResponse>> findPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) FacultyStatus status,
            @RequestParam(required = false) String documentReview,
            @PageableDefault(size = 25, sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(facultyService.findPage(search, specialityId, status, documentReview, pageable));
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('FACULTY_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) FacultyStatus status,
            @RequestParam(required = false) String documentReview,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, EXPORT_SORT_FIELDS.keySet(), "firstName", Sort.Direction.ASC);
        List<FacultyResponse> data = facultyService.findAll(search, specialityId, status, documentReview, exportSort);

        String specialityLabel = specialityId != null
            ? specialityRepository.findById(specialityId).map(Speciality::getName).orElse(null) : null;
        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "firstName", Sort.Direction.ASC);
        ExportMetadata meta = ExportMetadata.of("Faculty Members Export")
            .filter("Search", search)
            .filter("Speciality", specialityLabel)
            .filter("Status", status != null ? status.name() : null)
            .filter("Document Review", (documentReview != null && !documentReview.equalsIgnoreCase("ALL")) ? documentReview : null)
            .sort(EXPORT_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "faculty",
            () -> facultyExportService.toExcel(data, meta),
            () -> facultyExportService.toPdf(data, meta));
    }

    @GetMapping("/nrts-exists")
    public ResponseEntity<Boolean> nrtsExists(
            @RequestParam String value,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(facultyService.nrtsNumberExists(value, excludeId));
    }

}
