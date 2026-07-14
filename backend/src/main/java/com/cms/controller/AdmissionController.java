package com.cms.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DocumentChecklistResponse;
import com.cms.dto.DocumentFileDownload;

import com.cms.dto.AcademicQualificationRequest;
import com.cms.dto.AcademicQualificationResponse;
import com.cms.dto.AdmissionConfirmationDto;
import com.cms.dto.AdmissionDocumentResponse;
import com.cms.dto.AdmissionRequest;
import com.cms.dto.AdmissionResponse;
import com.cms.model.AcademicYear;
import com.cms.model.Course;
import com.cms.model.Program;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.ProgramRepository;
import com.cms.service.AcademicQualificationService;
import com.cms.service.AdmissionDocumentService;
import com.cms.service.AdmissionExportService;
import com.cms.service.AdmissionService;
import com.cms.util.ExportSortUtils;
import com.cms.util.export.ExportMetadata;
import com.cms.util.export.ExportResponseFactory;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admissions")
public class AdmissionController {

    private static final Map<String, String> EXPORT_SORT_FIELDS = new LinkedHashMap<>();
    static {
        EXPORT_SORT_FIELDS.put("student.firstName", "Student Name");
        EXPORT_SORT_FIELDS.put("student.admissionNumber", "Admission No.");
        EXPORT_SORT_FIELDS.put("student.rollNumber", "Roll No.");
        EXPORT_SORT_FIELDS.put("student.program.name", "Program");
        EXPORT_SORT_FIELDS.put("student.course.name", "Course");
        EXPORT_SORT_FIELDS.put("student.semester", "Semester");
        EXPORT_SORT_FIELDS.put("applicationDate", "Application Date");
        EXPORT_SORT_FIELDS.put("joiningAcademicYear.name", "Joining Year");
        EXPORT_SORT_FIELDS.put("student.status", "Student Status");
        EXPORT_SORT_FIELDS.put("declarationDate", "Declaration Date");
    }

    private final AdmissionService admissionService;
    private final AcademicQualificationService academicQualificationService;
    private final AdmissionDocumentService admissionDocumentService;
    private final AdmissionExportService admissionExportService;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;

    public AdmissionController(AdmissionService admissionService,
                               AcademicQualificationService academicQualificationService,
                               AdmissionDocumentService admissionDocumentService,
                               AdmissionExportService admissionExportService,
                               ProgramRepository programRepository,
                               CourseRepository courseRepository,
                               AcademicYearRepository academicYearRepository) {
        this.admissionService = admissionService;
        this.academicQualificationService = academicQualificationService;
        this.admissionDocumentService = admissionDocumentService;
        this.admissionExportService = admissionExportService;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.academicYearRepository = academicYearRepository;
    }

    @PostMapping
    @PreAuthorize("@perm.has('ADMISSION_COMPLETE')")
    public ResponseEntity<AdmissionResponse> create(@Valid @RequestBody AdmissionRequest request) {
        AdmissionResponse response = admissionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AdmissionResponse>> findAll() {
        return ResponseEntity.ok(admissionService.findAll());
    }

    @GetMapping("/explorer")
    public ResponseEntity<Page<AdmissionResponse>> findExplorer(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "student.admissionNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(admissionService.findExplorer(
            programId, courseId, academicYearId, status, studentType, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdmissionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(admissionService.findById(id));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<AdmissionResponse> findByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(admissionService.findByStudentId(studentId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.hasAny('ADMISSION_COMPLETE', 'ADMISSION_EDIT')")
    public ResponseEntity<AdmissionResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody AdmissionRequest request) {
        return ResponseEntity.ok(admissionService.update(id, request));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('ADMISSION_EDIT')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        admissionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@perm.has('ADMISSION_COMPLETE')")
    public ResponseEntity<AdmissionConfirmationDto> confirm(@PathVariable Long id,
                                                            @RequestParam LocalDate admissionDate) {
        return ResponseEntity.ok(admissionService.confirm(id, admissionDate));
    }

    @PostMapping("/{admissionId}/qualifications")
    @PreAuthorize("@perm.has('ADMISSION_EDIT')")
    public ResponseEntity<AcademicQualificationResponse> addQualification(
            @PathVariable Long admissionId,
            @Valid @RequestBody AcademicQualificationRequest request) {
        AcademicQualificationResponse response = academicQualificationService.addQualification(admissionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{admissionId}/qualifications")
    public ResponseEntity<List<AcademicQualificationResponse>> findQualificationsByAdmissionId(
            @PathVariable Long admissionId) {
        return ResponseEntity.ok(academicQualificationService.findByAdmissionId(admissionId));
    }

    @DeleteMapping("/qualifications/{id}")
    @PreAuthorize("@perm.has('ADMISSION_EDIT')")
    public ResponseEntity<Void> deleteQualification(@PathVariable Long id) {
        academicQualificationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{admissionId}/documents")
    public ResponseEntity<List<AdmissionDocumentResponse>> findDocumentsByAdmissionId(
            @PathVariable Long admissionId) {
        return ResponseEntity.ok(admissionDocumentService.findByAdmissionId(admissionId));
    }

    @PatchMapping("/documents/{id}/verify")
    @PreAuthorize("@perm.has('ADMISSION_EDIT')")
    public ResponseEntity<AdmissionDocumentResponse> updateVerification(
            @PathVariable Long id,
            @RequestParam DocumentVerificationStatus status,
            @RequestParam String verifiedBy) {
        return ResponseEntity.ok(admissionDocumentService.updateVerification(id, status, verifiedBy));
    }

    @GetMapping("/{admissionId}/documents/checklist")
    public ResponseEntity<DocumentChecklistResponse> getChecklist(
            @PathVariable Long admissionId) {
        return ResponseEntity.ok(admissionDocumentService.getChecklist(admissionId));
    }

    @PostMapping(value = "/{admissionId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.has('DOCUMENT_SUBMISSION_MANAGE')")
    public ResponseEntity<AdmissionDocumentResponse> uploadDocument(
            @PathVariable Long admissionId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(admissionDocumentService.uploadFile(admissionId, documentType, remarks, file, force));
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        DocumentFileDownload download = admissionDocumentService.getFileForDownload(id);
        ByteArrayResource resource = new ByteArrayResource(download.data());
        String encoded = URLEncoder.encode(download.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "inline; filename=\"" + download.fileName().replaceAll("[\\\\\"\\r\\n]", "_")
            + "\"; filename*=UTF-8''" + encoded;
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .contentType(MediaType.parseMediaType(download.contentType()))
            .contentLength(download.data().length)
            .body(resource);
    }

    @GetMapping("/export")
    @PreAuthorize("@perm.has('ADMISSION_EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {

        Sort exportSort = ExportSortUtils.resolve(
            sort, direction, EXPORT_SORT_FIELDS.keySet(), "student.admissionNumber", Sort.Direction.ASC);
        List<AdmissionResponse> data = admissionService.findExplorerAll(
            programId, courseId, academicYearId, status, studentType, search, exportSort);

        String programLabel = programId != null ? programRepository.findById(programId).map(Program::getName).orElse(null) : null;
        String courseLabel = courseId != null ? courseRepository.findById(courseId).map(Course::getName).orElse(null) : null;
        String academicYearLabel = academicYearId != null
            ? academicYearRepository.findById(academicYearId).map(AcademicYear::getName).orElse(null) : null;

        Sort.Order order = ExportSortUtils.firstOrder(exportSort, "student.admissionNumber", Sort.Direction.ASC);
        ExportMetadata meta = ExportMetadata.of("Student Admissions Export")
            .filter("Search", search)
            .filter("Program", programLabel)
            .filter("Course", courseLabel)
            .filter("Academic Year", academicYearLabel)
            .filter("Status", status)
            .filter("Student Type", studentType)
            .sort(EXPORT_SORT_FIELDS.get(order.getProperty()), order.getDirection());

        return ExportResponseFactory.respond(format, "admissions",
            () -> admissionExportService.toExcel(data, meta),
            () -> admissionExportService.toPdf(data, meta));
    }
}
