package com.cms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.AcademicQualificationRequest;
import com.cms.dto.AcademicQualificationResponse;
import com.cms.dto.AdmissionDocumentResponse;
import com.cms.dto.AdmissionRequest;
import com.cms.dto.AdmissionResponse;
import com.cms.model.enums.AdmissionStatus;
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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdmissionResponse> create(@Valid @RequestBody AdmissionRequest request) {
        AdmissionResponse response = admissionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AdmissionResponse>> findAll() {
        return ResponseEntity.ok(admissionService.findAll());
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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdmissionResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody AdmissionRequest request) {
        return ResponseEntity.ok(admissionService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdmissionResponse> updateStatus(@PathVariable Long id,
                                                          @RequestParam AdmissionStatus status) {
        return ResponseEntity.ok(admissionService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        admissionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{admissionId}/qualifications")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AdmissionDocumentResponse> updateVerification(
            @PathVariable Long id,
            @RequestParam DocumentVerificationStatus status,
            @RequestParam String verifiedBy) {
        return ResponseEntity.ok(admissionDocumentService.updateVerification(id, status, verifiedBy));
    }

    @GetMapping("/{admissionId}/documents/checklist")
    public ResponseEntity<Map<DocumentType, DocumentVerificationStatus>> getChecklist(
            @PathVariable Long admissionId) {
        return ResponseEntity.ok(admissionDocumentService.getChecklist(admissionId));
    }
}
