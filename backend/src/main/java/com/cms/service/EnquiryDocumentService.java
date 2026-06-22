package com.cms.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.config.PermSecurityBean;
import com.cms.dto.DocumentFileDownload;
import com.cms.dto.DocumentVerificationStatusResponse;
import com.cms.dto.EnquiryDocumentHistoryResponse;
import com.cms.dto.EnquiryDocumentRequest;
import com.cms.dto.EnquiryDocumentResponse;
import com.cms.dto.MissingDocumentsResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryDocument;
import com.cms.model.EnquiryDocumentHistory;
import com.cms.model.EnquiryStatusHistory;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.model.enums.EnquiryStatus;
import com.cms.repository.EnquiryDocumentHistoryRepository;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EnquiryStatusHistoryRepository;
import com.cms.util.CurrentUserResolver;

@Service
@Transactional(readOnly = true)
public class EnquiryDocumentService {

    private final EnquiryDocumentRepository documentRepository;
    private final EnquiryDocumentHistoryRepository historyRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryStatusHistoryRepository statusHistoryRepository;
    private final CurrentUserResolver currentUserResolver;
    private final PermSecurityBean permSecurityBean;

    public EnquiryDocumentService(EnquiryDocumentRepository documentRepository,
                                   EnquiryDocumentHistoryRepository historyRepository,
                                   EnquiryRepository enquiryRepository,
                                   EnquiryStatusHistoryRepository statusHistoryRepository,
                                   CurrentUserResolver currentUserResolver,
                                   PermSecurityBean permSecurityBean) {
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.enquiryRepository = enquiryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.currentUserResolver = currentUserResolver;
        this.permSecurityBean = permSecurityBean;
    }

    public List<EnquiryDocumentHistoryResponse> getHistory(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document not found with id: " + documentId);
        }
        return historyRepository.findByEnquiryDocumentIdOrderByChangedAtDesc(documentId).stream()
            .map(this::toHistoryResponse)
            .toList();
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
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        List<EnquiryDocument> documents = documentRepository.findByEnquiryId(enquiryId);

        Set<DocumentType> submittedTypes = documents.stream()
            .filter(d -> d.getStatus() == DocumentVerificationStatus.UPLOADED
                || d.getStatus() == DocumentVerificationStatus.VERIFIED)
            .map(EnquiryDocument::getDocumentType)
            .collect(Collectors.toSet());

        Set<DocumentType> mandatory = resolveMandatoryTypes(enquiry);

        List<String> missing = new ArrayList<>();
        for (DocumentType type : mandatory) {
            if (!submittedTypes.contains(type)) {
                missing.add(type.name());
            }
        }

        return new MissingDocumentsResponse(missing.isEmpty(), missing);
    }

    /**
     * Gate: all mandatory docs must be VERIFIED; any uploaded optional docs must
     * also be VERIFIED (an optional doc stuck at UPLOADED blocks the gate).
     */
    public DocumentVerificationStatusResponse allMandatoryDocumentsVerified(Long enquiryId) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        List<EnquiryDocument> documents = documentRepository.findByEnquiryId(enquiryId);

        Set<DocumentType> verifiedTypes = documents.stream()
            .filter(d -> d.getStatus() == DocumentVerificationStatus.VERIFIED)
            .map(EnquiryDocument::getDocumentType)
            .collect(Collectors.toSet());

        Set<DocumentType> uploadedTypes = documents.stream()
            .filter(d -> d.getStatus() == DocumentVerificationStatus.UPLOADED)
            .map(EnquiryDocument::getDocumentType)
            .collect(Collectors.toSet());

        Set<DocumentType> mandatory = resolveMandatoryTypes(enquiry);
        Set<DocumentType> optional  = resolveOptionalTypes(enquiry);

        List<String> unverified = new ArrayList<>();
        List<String> notUploaded = new ArrayList<>();

        // All mandatory docs must be verified.
        for (DocumentType type : mandatory) {
            if (!verifiedTypes.contains(type)) {
                unverified.add(type.name());
                if (!uploadedTypes.contains(type)) {
                    notUploaded.add(type.name());
                }
            }
        }

        // Any uploaded optional doc must also be verified.
        for (DocumentType type : optional) {
            if (uploadedTypes.contains(type)) {
                unverified.add(type.name());
            }
        }

        return new DocumentVerificationStatusResponse(
            unverified.isEmpty(), notUploaded.isEmpty(), unverified, notUploaded);
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

        DocumentVerificationStatus previousStatus = document.getStatus();

        document.setDocumentType(request.documentType());
        if (request.status() != null) {
            document.setStatus(request.status());
        }
        document.setRemarks(request.remarks());

        EnquiryDocument updated = documentRepository.save(document);

        if (request.status() != null && request.status() != previousStatus) {
            recordHistory(updated, previousStatus, updated.getStatus());
        }

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
    public EnquiryDocumentResponse uploadFile(Long enquiryId,
                                               DocumentType documentType,
                                               String remarks,
                                               MultipartFile file) {
        return uploadFile(enquiryId, documentType, remarks, file, false);
    }

    @Transactional
    public EnquiryDocumentResponse uploadFile(Long enquiryId,
                                               DocumentType documentType,
                                               String remarks,
                                               MultipartFile file,
                                               boolean forceOverride) {
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

        boolean wasVerified = document.getId() != null && document.getStatus() == DocumentVerificationStatus.VERIFIED;
        if (wasVerified) {
            if (!forceOverride) {
                throw new IllegalStateException("Cannot replace a verified document");
            }
            if (!permSecurityBean.has("DOCUMENT_VERIFIED_OVERRIDE")) {
                throw new AccessDeniedException("Replacing a verified document requires override permission");
            }
        }

        DocumentVerificationStatus previousStatus = document.getId() != null ? document.getStatus() : null;

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
        if (wasVerified) {
            document.setVerifiedBy(null);
            document.setVerifiedAt(null);
        }
        // Always overwrite remarks so a previous rejection reason is cleared
        // from the active record. The old value is preserved in history via
        // the recordHistory call below.
        document.setRemarks(remarks);

        EnquiryDocument saved = documentRepository.save(document);
        recordHistory(saved, previousStatus, DocumentVerificationStatus.UPLOADED);
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
     * Marks a document as VERIFIED and, if all program-required documents are now
     * verified, automatically transitions the enquiry to DOCUMENTS_VERIFIED.
     */
    @Transactional
    public EnquiryDocumentResponse verifyDocument(Long enquiryId, Long documentId) {
        EnquiryDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!document.getEnquiry().getId().equals(enquiryId)) {
            throw new ResourceNotFoundException("Document " + documentId + " does not belong to enquiry " + enquiryId);
        }

        DocumentVerificationStatus previousStatus = document.getStatus();
        String verifier = currentUserResolver.resolve();

        document.setStatus(DocumentVerificationStatus.VERIFIED);
        document.setVerifiedBy(verifier);
        document.setVerifiedAt(Instant.now());
        document.setRemarks(null);

        EnquiryDocument saved = documentRepository.save(document);
        recordHistory(saved, previousStatus, DocumentVerificationStatus.VERIFIED);

        autoTransitionIfAllVerified(saved.getEnquiry(), verifier);

        return toResponse(saved);
    }

    /**
     * Marks a document as REJECTED with a mandatory rejection comment.
     */
    @Transactional
    public EnquiryDocumentResponse rejectDocument(Long enquiryId, Long documentId, String rejectionComment) {
        if (rejectionComment == null || rejectionComment.isBlank()) {
            throw new IllegalArgumentException("Rejection comment is required");
        }

        EnquiryDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!document.getEnquiry().getId().equals(enquiryId)) {
            throw new ResourceNotFoundException("Document " + documentId + " does not belong to enquiry " + enquiryId);
        }

        DocumentVerificationStatus previousStatus = document.getStatus();

        document.setStatus(DocumentVerificationStatus.REJECTED);
        document.setRemarks(rejectionComment.trim());
        document.setVerifiedBy(currentUserResolver.resolve());
        document.setVerifiedAt(Instant.now());

        EnquiryDocument saved = documentRepository.save(document);
        recordHistory(saved, previousStatus, DocumentVerificationStatus.REJECTED);

        return toResponse(saved);
    }

    /**
     * Public entry-point that re-evaluates whether all required documents are verified
     * and transitions the enquiry to DOCUMENTS_VERIFIED if so.
     * <p>
     * Used as a fallback when no individual {@link #verifyDocument} calls are made —
     * e.g. when the program has no required document types configured, so the
     * auto-transition inside {@code verifyDocument} is never triggered.
     * </p>
     */
    @Transactional
    public void completeVerification(Long enquiryId) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));
        autoTransitionIfAllVerified(enquiry, currentUserResolver.resolve());
    }

    /**
     * Auto-transitions to DOCUMENTS_VERIFIED when all mandatory docs are verified
     * and no uploaded optional docs are left pending verification.
     */
    private void autoTransitionIfAllVerified(Enquiry enquiry, String changedBy) {
        if (enquiry.getStatus() != EnquiryStatus.DOCUMENTS_SUBMITTED) {
            return;
        }

        Set<DocumentType> mandatory = resolveMandatoryTypes(enquiry);
        Set<DocumentType> optional  = resolveOptionalTypes(enquiry);
        List<EnquiryDocument> docs = documentRepository.findByEnquiryId(enquiry.getId());

        Set<DocumentType> verifiedTypes = docs.stream()
            .filter(d -> d.getStatus() == DocumentVerificationStatus.VERIFIED)
            .map(EnquiryDocument::getDocumentType)
            .collect(Collectors.toSet());

        Set<DocumentType> uploadedTypes = docs.stream()
            .filter(d -> d.getStatus() == DocumentVerificationStatus.UPLOADED)
            .map(EnquiryDocument::getDocumentType)
            .collect(Collectors.toSet());

        boolean allMandatoryVerified = verifiedTypes.containsAll(mandatory);
        boolean noUploadedOptionalPending = optional.stream().noneMatch(uploadedTypes::contains);

        if (allMandatoryVerified && noUploadedOptionalPending) {
            EnquiryStatus oldStatus = enquiry.getStatus();
            enquiry.setStatus(EnquiryStatus.DOCUMENTS_VERIFIED);
            enquiryRepository.save(enquiry);
            statusHistoryRepository.save(
                new EnquiryStatusHistory(enquiry, oldStatus, EnquiryStatus.DOCUMENTS_VERIFIED, changedBy, null)
            );
        }
    }

    private Set<DocumentType> resolveMandatoryTypes(Enquiry enquiry) {
        if (enquiry.getProgram() != null) {
            return new HashSet<>(enquiry.getProgram().getMandatoryDocumentTypes());
        }
        return new HashSet<>();
    }

    private Set<DocumentType> resolveOptionalTypes(Enquiry enquiry) {
        if (enquiry.getProgram() != null) {
            return new HashSet<>(enquiry.getProgram().getOptionalDocumentTypes());
        }
        return new HashSet<>();
    }

    private void recordHistory(EnquiryDocument doc, DocumentVerificationStatus previous,
                               DocumentVerificationStatus next) {
        EnquiryDocumentHistory history = new EnquiryDocumentHistory();
        history.setEnquiryDocument(doc);
        history.setEnquiry(doc.getEnquiry());
        history.setAdmission(doc.getAdmission());
        history.setDocumentType(doc.getDocumentType());
        history.setPreviousStatus(previous);
        history.setNewStatus(next);
        history.setFileName(doc.getFileName());
        history.setFileSize(doc.getFileSize());
        history.setContentType(doc.getContentType());
        history.setRemarks(doc.getRemarks());
        history.setChangedBy(currentUserResolver.resolve());
        historyRepository.save(history);
    }

    private EnquiryDocumentHistoryResponse toHistoryResponse(EnquiryDocumentHistory h) {
        DocumentType dt = h.getDocumentType();
        Long enquiryId = h.getEnquiry() != null ? h.getEnquiry().getId() : null;
        Long admissionId = h.getAdmission() != null ? h.getAdmission().getId() : null;
        return new EnquiryDocumentHistoryResponse(
            h.getId(),
            h.getEnquiryDocument().getId(),
            enquiryId,
            admissionId,
            dt,
            dt != null ? dt.getDisplayName() : null,
            h.getPreviousStatus(),
            h.getNewStatus(),
            h.getFileName(),
            h.getFileSize(),
            h.getContentType(),
            h.getRemarks(),
            h.getChangedBy(),
            h.getChangedAt()
        );
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

