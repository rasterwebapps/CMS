package com.cms.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.service.AcademicQualificationService;
import com.cms.service.AdmissionDocumentService;
import com.cms.service.AdmissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admissions")
public class AdmissionController {

    private final AdmissionService admissionService;
    private final AcademicQualificationService academicQualificationService;
    private final AdmissionDocumentService admissionDocumentService;

    public AdmissionController(AdmissionService admissionService,
                               AcademicQualificationService academicQualificationService,
                               AdmissionDocumentService admissionDocumentService) {
        this.admissionService = admissionService;
        this.academicQualificationService = academicQualificationService;
        this.admissionDocumentService = admissionDocumentService;
    }

    @PostMapping
    @PreAuthorize("@perm.has('ADMISSION_CREATE')")
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
    @PreAuthorize("@perm.has('ADMISSION_CREATE')")
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
    @PreAuthorize("@perm.has('ADMISSION_CREATE')")
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
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(admissionDocumentService.uploadFile(admissionId, documentType, remarks, file));
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
}
