package com.cms.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DocumentFileDownload;
import com.cms.dto.FacultyDocumentHistoryResponse;
import com.cms.dto.FacultyDocumentRequest;
import com.cms.dto.FacultyDocumentResponse;
import com.cms.dto.FacultyPendingDocumentsSummary;
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

    public FacultyDocumentService(FacultyDocumentRepository documentRepository,
                                   FacultyDocumentHistoryRepository historyRepository,
                                   FacultyRepository facultyRepository,
                                   CurrentUserResolver currentUserResolver) {
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.facultyRepository = facultyRepository;
        this.currentUserResolver = currentUserResolver;
    }

    public List<FacultyDocumentResponse> findByFacultyId(Long facultyId) {
        if (!facultyRepository.existsById(facultyId)) {
            throw new ResourceNotFoundException("Faculty not found with id: " + facultyId);
        }
        return documentRepository.findByFacultyId(facultyId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<FacultyPendingDocumentsSummary> findPendingSummaries() {
        return documentRepository.findByStatus(DocumentVerificationStatus.UPLOADED).stream()
            .collect(java.util.stream.Collectors.groupingBy(
                doc -> doc.getFaculty().getId(),
                java.util.LinkedHashMap::new,
                java.util.stream.Collectors.toList()
            ))
            .entrySet().stream()
            .map(entry -> {
                var faculty = entry.getValue().get(0).getFaculty();
                return new FacultyPendingDocumentsSummary(
                    faculty.getId(),
                    faculty.getFullName(),
                    faculty.getEmployeeCode(),
                    faculty.getDepartment().getName(),
                    faculty.getDesignation().name(),
                    entry.getValue().size()
                );
            })
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

        document.setDocumentType(request.documentType());
        if (request.status() != null) {
            document.setStatus(request.status());
        }
        document.setRemarks(request.remarks());

        FacultyDocument saved = documentRepository.save(document);

        if (request.status() != null && request.status() != previousStatus) {
            recordHistory(saved, previousStatus, saved.getStatus());
        }

        return toResponse(saved);
    }

    @Transactional
    public void deleteDocument(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document not found with id: " + id);
        }
        documentRepository.deleteById(id);
    }

    @Transactional
    public FacultyDocumentResponse uploadFile(Long facultyId, DocumentType documentType,
                                               String remarks, MultipartFile file) {
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
