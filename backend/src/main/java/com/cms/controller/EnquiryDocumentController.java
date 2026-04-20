package com.cms.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.EnquiryDocumentRequest;
import com.cms.dto.EnquiryDocumentResponse;
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
}
