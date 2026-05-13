package com.cms.service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.AdmissionDocumentResponse;
import com.cms.dto.DocumentFileDownload;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Admission;
import com.cms.model.EnquiryDocument;
import com.cms.model.EnquiryDocumentHistory;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.DocumentType;
import com.cms.model.enums.DocumentVerificationStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryDocumentHistoryRepository;
import com.cms.repository.EnquiryDocumentRepository;
import com.cms.util.CurrentUserResolver;

@Service
@Transactional(readOnly = true)
public class AdmissionDocumentService {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final EnquiryDocumentRepository documentRepository;
    private final EnquiryDocumentHistoryRepository historyRepository;
    private final AdmissionRepository admissionRepository;
    private final CurrentUserResolver currentUserResolver;

    public AdmissionDocumentService(EnquiryDocumentRepository documentRepository,
                                    EnquiryDocumentHistoryRepository historyRepository,
                                    AdmissionRepository admissionRepository,
                                    CurrentUserResolver currentUserResolver) {
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.admissionRepository = admissionRepository;
        this.currentUserResolver = currentUserResolver;
    }

    public List<AdmissionDocumentResponse> findByAdmissionId(Long admissionId) {
        return documentRepository.findByAdmission_Id(admissionId).stream()
            .map(doc -> toResponse(doc, admissionId))
            .toList();
    }

    @Transactional
    public AdmissionDocumentResponse uploadFile(Long admissionId, DocumentType documentType,
                                                String remarks, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds the 10 MB maximum");
        }

        Admission admission = admissionRepository.findById(admissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + admissionId));

        EnquiryDocument document = documentRepository.findByAdmission_Id(admissionId).stream()
            .filter(d -> d.getDocumentType() == documentType)
            .findFirst()
            .orElseGet(() -> {
                EnquiryDocument d = new EnquiryDocument(null, documentType, DocumentVerificationStatus.NOT_UPLOADED);
                d.setAdmission(admission);
                return d;
            });

        if (document.getId() != null && document.getStatus() == DocumentVerificationStatus.VERIFIED) {
            throw new IllegalStateException("Cannot replace a verified document");
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
        if (remarks != null && !remarks.isBlank()) {
            document.setRemarks(remarks.trim());
        }

        EnquiryDocument saved = documentRepository.save(document);
        recordHistory(saved, previousStatus, DocumentVerificationStatus.UPLOADED);

        return toResponse(saved, admissionId);
    }

    public DocumentFileDownload getFileForDownload(Long documentId) {
        EnquiryDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        byte[] data = document.getFileData();
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("No file uploaded for document id: " + documentId);
        }
        String fileName = document.getFileName() != null
            ? document.getFileName() : document.getDocumentType().name();
        String contentType = document.getContentType() != null
            ? document.getContentType() : "application/octet-stream";
        return new DocumentFileDownload(fileName, contentType, data);
    }

    @Transactional
    public AdmissionDocumentResponse updateVerification(Long docId, DocumentVerificationStatus status,
                                                        String verifiedBy) {
        EnquiryDocument document = documentRepository.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + docId));

        if (document.getStatus() == DocumentVerificationStatus.VERIFIED) {
            throw new IllegalStateException("Document is already verified and cannot be changed");
        }

        DocumentVerificationStatus previousStatus = document.getStatus();

        document.setStatus(status);
        document.setVerifiedBy(verifiedBy != null ? verifiedBy : currentUserResolver.resolve());
        document.setVerifiedAt(Instant.now());
        document.setRemarks(null);

        EnquiryDocument updated = documentRepository.save(document);
        recordHistory(updated, previousStatus, status);

        Long admissionId = updated.getAdmission() != null ? updated.getAdmission().getId() : null;
        return toResponse(updated, admissionId);
    }

    public Map<DocumentType, DocumentVerificationStatus> getChecklist(Long admissionId) {
        Admission admission = admissionRepository.findById(admissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + admissionId));

        Set<DocumentType> applicable = resolveApplicableTypes(admission);

        Map<DocumentType, DocumentVerificationStatus> checklist = new EnumMap<>(DocumentType.class);
        for (DocumentType type : applicable) {
            checklist.put(type, DocumentVerificationStatus.NOT_UPLOADED);
        }
        for (EnquiryDocument doc : documentRepository.findByAdmission_Id(admissionId)) {
            if (applicable.contains(doc.getDocumentType())) {
                checklist.put(doc.getDocumentType(), doc.getStatus());
            }
        }
        return checklist;
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

    private Set<DocumentType> resolveApplicableTypes(Admission admission) {
        Student student = admission.getStudent();
        if (student != null) {
            Program program = student.getProgram();
            if (program != null) {
                Set<DocumentType> configured = program.getRequiredDocumentTypes();
                if (configured != null && !configured.isEmpty()) {
                    return new HashSet<>(configured);
                }
            }
        }
        return new HashSet<>(Set.of(DocumentType.values()));
    }

    private String sanitizeFileName(String original) {
        if (original == null) return null;
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        return name.isBlank() ? null : name;
    }

    private AdmissionDocumentResponse toResponse(EnquiryDocument document, Long admissionId) {
        Long resolvedAdmissionId = admissionId != null ? admissionId
            : (document.getAdmission() != null ? document.getAdmission().getId() : null);
        return new AdmissionDocumentResponse(
            document.getId(),
            resolvedAdmissionId,
            document.getDocumentType(),
            document.getFileName(),
            null,
            toUtcLocalDateTime(document.getUploadedAt()),
            null,
            document.getVerifiedBy(),
            toUtcLocalDateTime(document.getVerifiedAt()),
            document.getStatus(),
            document.getCreatedAt(),
            document.getUpdatedAt(),
            document.getFileName() != null,
            document.getRemarks(),
            document.getContentType(),
            document.getFileSize()
        );
    }

    private LocalDateTime toUtcLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
