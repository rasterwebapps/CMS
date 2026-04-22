package com.cms.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DocumentFileDownload;
import com.cms.dto.EnquiryDocumentRequest;
import com.cms.dto.EnquiryDocumentResponse;
import com.cms.dto.MissingDocumentsResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryDocument;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.EnquiryRepository;

@Service
@Transactional(readOnly = true)
public class EnquiryDocumentService {

    private static final Set<DocumentType> MANDATORY_DOCUMENTS = Set.of(
        DocumentType.TENTH_MARKSHEET,
        DocumentType.TWELFTH_MARKSHEET,
        DocumentType.TRANSFER_CERTIFICATE,
        DocumentType.AADHAR_CARD,
        DocumentType.PASSPORT_PHOTO
    );

    private final EnquiryDocumentRepository documentRepository;
    private final EnquiryRepository enquiryRepository;

    public EnquiryDocumentService(EnquiryDocumentRepository documentRepository,
                                   EnquiryRepository enquiryRepository) {
        this.documentRepository = documentRepository;
        this.enquiryRepository = enquiryRepository;
    }

    @Transactional
    public EnquiryDocumentResponse addDocument(Long enquiryId, EnquiryDocumentRequest request) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        DocumentVerificationStatus status = request.status() != null
            ? request.status() : DocumentVerificationStatus.NOT_UPLOADED;

        EnquiryDocument document = new EnquiryDocument(enquiry, request.documentType(), status);
        document.setRemarks(request.remarks());

        EnquiryDocument saved = documentRepository.save(document);

        return toResponse(saved);
    }

    public MissingDocumentsResponse allMandatoryDocumentsSubmitted(Long enquiryId) {
        if (!enquiryRepository.existsById(enquiryId)) {
            throw new ResourceNotFoundException("Enquiry not found with id: " + enquiryId);
        }

        List<EnquiryDocument> documents = documentRepository.findByEnquiryId(enquiryId);

        Set<DocumentType> submittedTypes = documents.stream()
            .filter(d -> d.getStatus() == DocumentVerificationStatus.UPLOADED
                || d.getStatus() == DocumentVerificationStatus.VERIFIED)
            .map(EnquiryDocument::getDocumentType)
            .collect(Collectors.toSet());

        List<String> missing = new ArrayList<>();
        for (DocumentType mandatory : MANDATORY_DOCUMENTS) {
            if (!submittedTypes.contains(mandatory)) {
                missing.add(mandatory.name());
            }
        }

        return new MissingDocumentsResponse(missing.isEmpty(), missing);
    }

    public List<EnquiryDocumentResponse> findByEnquiryId(Long enquiryId) {
        if (!enquiryRepository.existsById(enquiryId)) {
            throw new ResourceNotFoundException("Enquiry not found with id: " + enquiryId);
        }
        return documentRepository.findByEnquiryId(enquiryId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public EnquiryDocumentResponse updateDocument(Long id, EnquiryDocumentRequest request) {
        EnquiryDocument document = documentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        document.setDocumentType(request.documentType());
        if (request.status() != null) {
            document.setStatus(request.status());
        }
        document.setRemarks(request.remarks());

        EnquiryDocument updated = documentRepository.save(document);
        return toResponse(updated);
    }

    @Transactional
    public void deleteDocument(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document not found with id: " + id);
        }
        documentRepository.deleteById(id);
    }

    /** Maximum allowed upload size: 10 MB. */
    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    /**
     * Upserts an enquiry document of the given type with binary file content.
     * If a record already exists for that (enquiry, documentType) it is
     * updated in place; otherwise a new record is created. The verification
     * status is set to UPLOADED on successful upload.
     */
    @Transactional
    public EnquiryDocumentResponse uploadFile(Long enquiryId,
                                               DocumentType documentType,
                                               String remarks,
                                               MultipartFile file) {
        if (documentType == null) {
            throw new IllegalArgumentException("documentType is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "File exceeds maximum allowed size of " + MAX_FILE_SIZE_BYTES + " bytes"
            );
        }

        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        EnquiryDocument document = documentRepository.findByEnquiryId(enquiryId).stream()
            .filter(d -> d.getDocumentType() == documentType)
            .findFirst()
            .orElseGet(() -> new EnquiryDocument(enquiry, documentType, DocumentVerificationStatus.NOT_UPLOADED));

        try {
            document.setFileData(file.getBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded file", ex);
        }
        document.setFileName(sanitizeFileName(file.getOriginalFilename()));
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setUploadedAt(Instant.now());
        document.setStatus(DocumentVerificationStatus.UPLOADED);
        if (remarks != null) {
            document.setRemarks(remarks);
        }

        EnquiryDocument saved = documentRepository.save(document);
        return toResponse(saved);
    }

    /**
     * Loads the binary content of a stored document so that it can be
     * streamed to the client for viewing or downloading.
     */
    public DocumentFileDownload getFileForDownload(Long documentId) {
        EnquiryDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        byte[] data = document.getFileData();
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("No file uploaded for document id: " + documentId);
        }
        String fileName = document.getFileName() != null
            ? document.getFileName()
            : document.getDocumentType().name();
        String contentType = document.getContentType() != null
            ? document.getContentType()
            : "application/octet-stream";
        return new DocumentFileDownload(fileName, contentType, data);
    }

    /**
     * Strips any path components from an uploaded file name to avoid
     * Content-Disposition path traversal issues.
     */
    private String sanitizeFileName(String original) {
        if (original == null) {
            return null;
        }
        // Remove any directory components from the original filename.
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.isBlank() ? null : name;
    }

    private EnquiryDocumentResponse toResponse(EnquiryDocument doc) {
        Long fileSize = doc.getFileSize();
        boolean hasFile = doc.getFileName() != null;
        return new EnquiryDocumentResponse(
            doc.getId(),
            doc.getEnquiry().getId(),
            doc.getDocumentType(),
            doc.getStatus(),
            doc.getRemarks(),
            doc.getVerifiedBy(),
            doc.getVerifiedAt(),
            doc.getCreatedAt(),
            doc.getUpdatedAt(),
            doc.getFileName(),
            doc.getContentType(),
            fileSize,
            doc.getUploadedAt(),
            hasFile
        );
    }
}

