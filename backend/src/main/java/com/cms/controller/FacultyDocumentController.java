package com.cms.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.cms.dto.FacultyDocumentHistoryResponse;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DocumentFileDownload;
import com.cms.dto.FacultyDocumentRequest;
import com.cms.dto.FacultyDocumentResponse;
import com.cms.model.enums.DocumentType;
import com.cms.service.FacultyDocumentService;
import com.cms.service.FacultyDocumentTypeRequirementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/faculty/{facultyId}/documents")
public class FacultyDocumentController {

    private final FacultyDocumentService documentService;
    private final FacultyDocumentTypeRequirementService requirementService;

    public FacultyDocumentController(FacultyDocumentService documentService,
            FacultyDocumentTypeRequirementService requirementService) {
        this.documentService = documentService;
        this.requirementService = requirementService;
    }

    @GetMapping
    public ResponseEntity<List<FacultyDocumentResponse>> findByFacultyId(@PathVariable Long facultyId) {
        return ResponseEntity.ok(documentService.findByFacultyId(facultyId));
    }

    @GetMapping("/required-types")
    public ResponseEntity<Set<String>> getRequiredDocumentTypes(@PathVariable Long facultyId) {
        return ResponseEntity.ok(requirementService.getRequiredDocumentTypesForFaculty(facultyId));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<FacultyDocumentHistoryResponse>> getHistory(
            @PathVariable Long facultyId,
            @PathVariable Long id) {
        return ResponseEntity.ok(documentService.getHistory(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<FacultyDocumentResponse> addDocument(
            @PathVariable Long facultyId,
            @Valid @RequestBody FacultyDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(documentService.addDocument(facultyId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<FacultyDocumentResponse> updateDocument(
            @PathVariable Long facultyId,
            @PathVariable Long id,
            @Valid @RequestBody FacultyDocumentRequest request) {
        return ResponseEntity.ok(documentService.updateDocument(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long facultyId, @PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.has('FACULTY_MANAGE')")
    public ResponseEntity<FacultyDocumentResponse> uploadDocument(
            @PathVariable Long facultyId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(documentService.uploadFile(facultyId, documentType, remarks, file));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long facultyId,
            @PathVariable Long id) {
        DocumentFileDownload download = documentService.getFileForDownload(id);
        ByteArrayResource resource = new ByteArrayResource(download.data());

        String encoded = URLEncoder.encode(download.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "inline; filename=\"" + sanitizeForHeader(download.fileName())
            + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .contentType(MediaType.parseMediaType(download.contentType()))
            .contentLength(download.data().length)
            .body(resource);
    }

    private static String sanitizeForHeader(String name) {
        return name.replaceAll("[\\\\\"\\r\\n]", "_");
    }
}
