package com.cms.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.config.PermSecurityBean;
import com.cms.dto.DocumentFileDownload;
import com.cms.dto.FacultyDocumentHistoryResponse;
import com.cms.dto.FacultyDocumentRequest;
import com.cms.dto.FacultyDocumentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Faculty;
import com.cms.model.FacultyDocument;
import com.cms.model.FacultyDocumentHistory;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.FacultyDocumentHistoryRepository;
import com.cms.repository.FacultyDocumentRepository;
import com.cms.repository.FacultyRepository;
import com.cms.util.CurrentUserResolver;

@Service
@Transactional(readOnly = true)
public class FacultyDocumentService {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final FacultyDocumentRepository documentRepository;
    private final FacultyDocumentHistoryRepository historyRepository;
    private final FacultyRepository facultyRepository;
    private final CurrentUserResolver currentUserResolver;
    private final PermSecurityBean permSecurityBean;

    public FacultyDocumentService(FacultyDocumentRepository documentRepository,
                                   FacultyDocumentHistoryRepository historyRepository,
                                   FacultyRepository facultyRepository,
                                   CurrentUserResolver currentUserResolver,
                                   PermSecurityBean permSecurityBean) {
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.facultyRepository = facultyRepository;
        this.currentUserResolver = currentUserResolver;
        this.permSecurityBean = permSecurityBean;
    }

    public List<FacultyDocumentResponse> findByFacultyId(Long facultyId) {
        if (!facultyRepository.existsById(facultyId)) {
            throw new ResourceNotFoundException("Faculty not found with id: " + facultyId);
        }
        return documentRepository.findByFacultyId(facultyId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<FacultyDocumentHistoryResponse> getHistory(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document not found with id: " + documentId);
        }
        return historyRepository.findByFacultyDocumentIdOrderByChangedAtDesc(documentId).stream()
            .map(this::toHistoryResponse)
            .toList();
    }

    @Transactional
    public FacultyDocumentResponse addDocument(Long facultyId, FacultyDocumentRequest request) {
        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));

        DocumentVerificationStatus status = request.status() != null
            ? request.status() : DocumentVerificationStatus.NOT_UPLOADED;

        FacultyDocument document = new FacultyDocument(faculty, request.documentType(), status);
        document.setRemarks(request.remarks());
        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public FacultyDocumentResponse updateDocument(Long id, FacultyDocumentRequest request) {
        FacultyDocument document = documentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        DocumentVerificationStatus previousStatus = document.getStatus();
        DocumentVerificationStatus nextStatus = request.status();

        if (nextStatus == DocumentVerificationStatus.REJECTED && isBlank(request.remarks())) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        if (nextStatus == DocumentVerificationStatus.VERIFIED && !hasFile(document)) {
            throw new IllegalArgumentException("Cannot verify a document before a file is uploaded");
        }

        document.setDocumentType(request.documentType());
        if (nextStatus != null) {
            document.setStatus(nextStatus);
            if (nextStatus == DocumentVerificationStatus.VERIFIED) {
                document.setVerifiedBy(currentUserResolver.resolve());
                document.setVerifiedAt(Instant.now());
            } else if (previousStatus == DocumentVerificationStatus.VERIFIED) {
                document.setVerifiedBy(null);
                document.setVerifiedAt(null);
            }
        }
        document.setRemarks(request.remarks());

        FacultyDocument saved = documentRepository.save(document);

        if (nextStatus != null && nextStatus != previousStatus) {
            recordHistory(saved, previousStatus, saved.getStatus());
        }

        return toResponse(saved);
    }

    @Transactional
    public void deleteDocument(Long id) {
        FacultyDocument document = documentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        if (document.getStatus() == DocumentVerificationStatus.VERIFIED) {
            throw new IllegalStateException("Verified documents cannot be deleted");
        }
        documentRepository.deleteById(id);
    }

    public FacultyDocumentResponse uploadFile(Long facultyId, DocumentType documentType,
                                               String remarks, MultipartFile file) {
        return uploadFile(facultyId, documentType, remarks, file, false);
    }

    @Transactional
    public FacultyDocumentResponse uploadFile(Long facultyId, DocumentType documentType,
                                               String remarks, MultipartFile file, boolean forceOverride) {
        if (documentType == null) {
            throw new IllegalArgumentException("documentType is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "File exceeds maximum allowed size of " + MAX_FILE_SIZE_BYTES + " bytes");
        }

        Faculty faculty = facultyRepository.findById(facultyId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));

        FacultyDocument document = documentRepository.findByFacultyId(facultyId).stream()
            .filter(d -> d.getDocumentType() == documentType)
            .findFirst()
            .orElseGet(() -> new FacultyDocument(faculty, documentType, DocumentVerificationStatus.NOT_UPLOADED));

        if (document.getStatus() == DocumentVerificationStatus.VERIFIED) {
            if (!forceOverride) {
                throw new IllegalStateException("Verified documents cannot be replaced");
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
        document.setVerifiedBy(null);
        document.setVerifiedAt(null);
        if (remarks != null) {
            document.setRemarks(remarks);
        }

        FacultyDocument saved = documentRepository.save(document);
        recordHistory(saved, previousStatus, DocumentVerificationStatus.UPLOADED);

        return toResponse(saved);
    }

    public DocumentFileDownload getFileForDownload(Long documentId) {
        FacultyDocument document = documentRepository.findById(documentId)
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

    private void recordHistory(FacultyDocument doc, DocumentVerificationStatus previous,
                                DocumentVerificationStatus next) {
        FacultyDocumentHistory history = new FacultyDocumentHistory();
        history.setFacultyDocument(doc);
        history.setFaculty(doc.getFaculty());
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

    private String sanitizeFileName(String original) {
        if (original == null) return null;
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        return name.isBlank() ? null : name;
    }

    private boolean hasFile(FacultyDocument document) {
        return document.getFileName() != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private FacultyDocumentResponse toResponse(FacultyDocument doc) {
        boolean hasFile = doc.getFileName() != null;
        return new FacultyDocumentResponse(
            doc.getId(),
            doc.getFaculty().getId(),
            doc.getDocumentType(),
            doc.getStatus(),
            doc.getRemarks(),
            doc.getVerifiedBy(),
            doc.getVerifiedAt(),
            doc.getCreatedAt(),
            doc.getUpdatedAt(),
            doc.getFileName(),
            doc.getContentType(),
            doc.getFileSize(),
            doc.getUploadedAt(),
            hasFile
        );
    }

    private FacultyDocumentHistoryResponse toHistoryResponse(FacultyDocumentHistory h) {
        DocumentType dt = h.getDocumentType();
        return new FacultyDocumentHistoryResponse(
            h.getId(),
            h.getFacultyDocument().getId(),
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
}
