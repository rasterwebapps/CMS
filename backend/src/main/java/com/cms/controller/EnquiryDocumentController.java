package com.cms.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
import com.cms.dto.EnquiryDocumentRequest;
import com.cms.dto.EnquiryDocumentResponse;
import com.cms.model.enums.DocumentType;
import com.cms.service.EnquiryDocumentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/enquiries/{enquiryId}/documents")
public class EnquiryDocumentController {

    private final EnquiryDocumentService documentService;

    public EnquiryDocumentController(EnquiryDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<EnquiryDocumentResponse> addDocument(
            @PathVariable Long enquiryId,
            @Valid @RequestBody EnquiryDocumentRequest request) {
        EnquiryDocumentResponse response = documentService.addDocument(enquiryId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EnquiryDocumentResponse>> findByEnquiryId(@PathVariable Long enquiryId) {
        List<EnquiryDocumentResponse> documents = documentService.findByEnquiryId(enquiryId);
        return ResponseEntity.ok(documents);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<EnquiryDocumentResponse> updateDocument(
            @PathVariable Long enquiryId,
            @PathVariable Long id,
            @Valid @RequestBody EnquiryDocumentRequest request) {
        EnquiryDocumentResponse response = documentService.updateDocument(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long enquiryId, @PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Uploads a scanned document file for the given enquiry. The document
     * record is upserted by document type — uploading the same type again
     * replaces the previously stored file.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FRONT_OFFICE')")
    public ResponseEntity<EnquiryDocumentResponse> uploadDocument(
            @PathVariable Long enquiryId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestPart("file") MultipartFile file) {
        EnquiryDocumentResponse response = documentService.uploadFile(enquiryId, documentType, remarks, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Streams the stored binary so callers can view it inline (when the
     * browser supports the MIME type) or download it.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long enquiryId,
            @PathVariable Long id) {
        DocumentFileDownload download = documentService.getFileForDownload(id);
        ByteArrayResource resource = new ByteArrayResource(download.data());

        // RFC 6266: include both ASCII fallback and UTF-8 encoded filename.
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
        // Strip characters that would break the quoted ASCII fallback.
        return name.replaceAll("[\\\\\"\\r\\n]", "_");
    }
}
